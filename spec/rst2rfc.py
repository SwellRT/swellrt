#!/bin/env python
# Copyright 2009 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Utility to convert ReST markup to XML for xml2rfc

Command-line utility that takes a single ReST file
and converts it into XML suitable for processing
by xml2rfc.
"""

try:
  import locale
  locale.setlocale(locale.LC_ALL, '')
except:
  pass

import docutils
import os
import os.path
import re
import sys
import time

from docutils import nodes, writers, languages
from docutils.core import publish_cmdline, default_description
from docutils.transforms import writer_aux
from types import ListType
from xml.sax.saxutils import escape

DOCTYPE = [ """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE rfc SYSTEM "http://xml.resource.org/authoring/rfc2629.dtd" >
"""]

AUTHOR_ITEMS = ['organization', 'contact']


class RFCTranslator(nodes.NodeVisitor):
  """
  Translates ReST into the XML consumed by xml2rfc.
  """

  def __init__(self, document):
    nodes.NodeVisitor.__init__(self, document)
    self.settings = settings = document.settings
    lcode = settings.language_code
    self.language = languages.get_language(lcode)
    self.body = []

    self.section_level = 0
    self.title = []
    self.subtitle = []
    self.context = []

    self.body_pre_docinfo = []
    self.in_document_front = False
    self.in_author = False
    self.in_middle = False
    self.in_pre_document = False
    self.in_infoitem = False
    self.in_citation = False
    # Closures to apply to incoming _text messsages
    self.textcl = []
    # A stack of booleans, determine whether to push a '<t>'
    # element around the next visited paragraph
    self.nextparagraph = []
    # A stack of booleans, determine whether to push a '<t>'
    # element around a paragraph
    self.paragraph = []
    self._empty_rfc_value = False 

  def starttag(self, node, tagname, suffix='\n', empty=0, **attributes):
    """
    Construct and return a start tag given a node tag name, and optional attributes.
    """
    atts = {}
    for (name, value) in attributes.items():
      atts[name] = value
    attlist = atts.items()
    attlist.sort()
    parts = [tagname]
    for name, value in attlist:
      assert value is not None
      if isinstance(value, ListType):
        values = [unicode(v) for v in value]
        parts.append('%s="%s"' % (name, ' '.join(values)))
      else:
        parts.append('%s="%s"' % (name, unicode(value)))
    if empty:
      infix = ' /'
    else:
      infix = ''
    return '<%s%s>' % (' '.join(parts), infix) + suffix

  def begintag(self, node, tagname, suffix='\n', empty=0, **attr):
    """
    Does what starttag does, but also automatically adds the
    closing tag to the context stack.
    """
    self.body.append(self.starttag(node, tagname, suffix, empty, **attr))
    self.context.append('</%s>' % tagname + suffix)

  def endtag(self):
    """
    Close the current element.
    """
    self.body.append(self.context.pop())

  def astext(self):
    return ''.join(DOCTYPE + self.body_pre_docinfo + self.body)

  def encode(self, text):
    """Encode special characters in `text` & return."""
    text = text.replace('&', '&amp;')
    text = text.replace('<', '&lt;')
    text = text.replace('"', '&quot;')
    text = text.replace('>', '&gt;')
    return text

  def visit_Text(self, node):
    if len(self.textcl):
      cl = self.textcl.pop()
      cl(node)
      return
    if self.in_infoitem:
      return
    elif self.in_pre_document:
      self.body_pre_docinfo.append(self.encode(node.astext()))
    else:
      self.body.append(self.encode(node.astext()))

  def depart_Text(self, node):
    pass

  def visit_abbreviation(self, node):
    pass

  def depart_abbreviation(self, node):
    pass

  def visit_acronym(self, node):
    pass

  def depart_acronym(self, node):
    pass

  def visit_address(self, node):
    pass

  def depart_address(self, node):
    pass

  def capture_author(self, node):
    fullname = node.astext()
    surname = node.astext().split(" ", 1)[1]
    initials = " ".join([n[0] + "." for n in fullname.split(" ", 1)])
    self.body.append("<author fullname='%s' surname='%s' initials='%s'>" % (fullname, surname, initials))
    self.context.append("</author>")

  def visit_author(self, node):
    if self.in_author:
      self.body.append(self.context.pop())
    self.in_author = True
    self.in_pre_document = False
    self.textcl.append(self.capture_author)

  def depart_author(self, node):
    pass

  def visit_authors(self, node):
    self.visit_docinfo_item(node, 'authors')

  def depart_authors(self, node):
    self.depart_docinfo_item()

  def visit_block_quote(self, node):
    pass

  def depart_block_quote(self, node):
    pass

  def visit_bullet_list(self, node):
    self.begintag(node, 't')
    self.begintag(node, 'list', style='symbols')

  def depart_bullet_list(self, node):
    self.endtag()
    self.endtag()

  def visit_caption(self, node):
    pass

  def depart_caption(self, node):
    pass

  def capture_citation(self, node):
    try:
      f = open(os.path.join('refs', node.astext() + '.xml'), 'r')
      xml = f.readlines()
      f.close()
      # TODO parse as XML and serialize back out, don't treat as just text
      self.body.extend(xml[1:])
    except IOError:
      sys.exit('Can not find source file for reference: '
              + node.astext())

  def visit_citation(self, node):
    if self.in_middle:
      self.in_middle = False
      self.endtag() # /section
      self.endtag() # /middle
      self.context.append("") # fix up the depart_section event
    if not self.in_citation:
      self.begintag(node, 'back')
      self.begintag(node, 'references')
      self.in_citation = True
    self.textcl.append(self.capture_citation)
    self.footnote_backrefs(node)

  def depart_citation(self, node):
    if not self.in_citation:
      self.textcl.pop()

  def visit_citation_reference(self, node):
    self.begintag(node, 'xref', '\n', False, target=node['refid'].upper())
    raise nodes.SkipChildren

  def depart_citation_reference(self, node):
    self.endtag()

  def visit_classifier(self, node):
    pass

  def depart_classifier(self, node):
    pass

  def visit_colspec(self, node):
    pass

  def depart_colspec(self, node):
    pass

  def write_colspecs(self):
    pass

  def visit_comment(self, node,
                    sub=re.compile('-(?=-)').sub):
    """Escape double-dashes in comment text."""
    self.body.append('<!-- %s -->\n' % sub('- ', node.astext()))
    # Content already processed:
    raise nodes.SkipNode

  def visit_compound(self, node):
    pass

  def depart_compound(self, node):
    pass

  def visit_container(self, node):
    pass

  def depart_container(self, node):
    pass

  def visit_contact(self, node):
    self.body.append('<address><email>')
    self.context.append('</email></address>\n')

  def depart_contact(self, node):
    self.body.append(self.context.pop())

  def visit_copyright(self, node):
    pass

  def depart_copyright(self, node):
    pass

  def visit_date(self, node):
    if self.in_author:
      self.in_author = False
      self.body.append(self.context.pop())
    self.body.append(
        '<date year="%s" month="%s"/>\n' % tuple(node.astext().split(' '))
        )

  def depart_date(self, node):
    del self.body[-1:]

  def visit_decoration(self, node):
    pass

  def depart_decoration(self, node):
    pass

  def visit_definition(self, node):
    self.nextparagraph.append(False)

  def depart_definition(self, node):
    self.endtag()

  def visit_definition_list(self, node):
    self.begintag(node, 't')
    self.begintag(node, 'list', style="hanging")

  def depart_definition_list(self, node):
    self.endtag()
    self.endtag()

  def visit_definition_list_item(self, node):
    pass

  def depart_definition_list_item(self, node):
    pass

  def visit_description(self, node):
    pass

  def depart_description(self, node):
    pass

  def visit_docinfo(self, node):
    pass

  def depart_docinfo(self, node):
    pass

  def visit_docinfo_item(self, node, name, meta=1):
    if self.in_author and name not in AUTHOR_ITEMS:
      self.in_author = False
    self.in_infoitem = True
    self.body.append('<%s>%s</%s>\n' % (name, node.astext(), name))

  def depart_docinfo_item(self):
    self.in_infoitem = False

  def visit_doctest_block(self, node):
    pass

  def depart_doctest_block(self, node):
    pass

  def visit_document(self, node):
    self.begintag(node, 'rfc')
    self.begintag(node, 'front')

  def depart_document(self, node):
    if self.in_citation:
      self.endtag() # /references
    self.endtag()
    self.endtag()
    assert not self.context, 'len(context) = %s' % len(self.context)

  def visit_emphasis(self, node):
    self.begintag(node, 'spanx', '', style='emph')

  def depart_emphasis(self, node):
    self.endtag()

  def visit_entry(self, node):
    pass

  def depart_entry(self, node):
    pass

  def visit_enumerated_list(self, node):
    pass

  def depart_enumerated_list(self, node):
    pass

  def visit_field(self, node):

    if self.in_pre_document:
      if node.children[0].astext() == 'private':
        self._empty_rfc_value = True
      self.body_pre_docinfo.append('<?rfc ')
    else:
      self.body.append('<rfc ')

  def depart_field(self, node):
    pass

  def visit_field_body(self, node):
    if self.in_pre_document:
      if self._empty_rfc_value:
        val = " "
        self._empty_rfc_value = False 
      else:
        val = node.astext()
      self.body_pre_docinfo.append('="%s"?>\n' % val)
      raise nodes.SkipNode

  def visit_field_list(self, node):
    pass

  def depart_field_list(self, node):
    pass

  def visit_field_name(self, node):
    pass

  def depart_field_name(self, node):
    pass

  def visit_figure(self, node):
    pass

  def depart_figure(self, node):
    pass

  def visit_footer(self, node):
    pass

  def depart_footer(self, node):
    pass

  def visit_footnote(self, node):
    pass

  def footnote_backrefs(self, node):
    pass

  def depart_footnote(self, node):
    pass

  def visit_footnote_reference(self, node):
    pass

  def depart_footnote_reference(self, node):
    pass

  def visit_generated(self, node):
    pass

  def depart_generated(self, node):
    pass

  def visit_header(self, node):
    pass

  def depart_header(self, node):
    pass

  def visit_image(self, node):
    pass

  def depart_image(self, node):
    pass

  def visit_inline(self, node):
    pass

  def depart_inline(self, node):
    pass

  def visit_label(self, node):
    pass

  def depart_label(self, node):
    pass

  def visit_legend(self, node):
    pass

  def depart_legend(self, node):
    pass

  def visit_line(self, node):
    pass

  def depart_line(self, node):
    pass

  def visit_line_block(self, node):
    pass

  def depart_line_block(self, node):
    pass

  def visit_list_item(self, node):
    pass

  def depart_list_item(self, node):
    pass

  def visit_literal(self, node):
    """Process text to prevent tokens from wrapping."""

    self.begintag(node, 'spanx', '', style='verb')
    text = node.astext()
    text = escape(text)
    self.body.append(text)
    self.endtag()

    raise nodes.SkipNode

  def visit_literal_block(self, node):
    self.begintag(node, 'figure')
    self.begintag(node, 'artwork')

  def depart_literal_block(self, node):
    self.endtag()
    self.endtag()

  def visit_meta(self, node):
    pass

  def depart_meta(self, node):
    pass

  def visit_option(self, node):
    pass

  def depart_option(self, node):
    pass

  def visit_option_argument(self, node):
    pass

  def depart_option_argument(self, node):
    pass

  def visit_option_group(self, node):
    pass

  def depart_option_group(self, node):
    pass

  def visit_option_list(self, node):
    pass

  def depart_option_list(self, node):
    pass

  def visit_option_list_item(self, node):
    pass

  def depart_option_list_item(self, node):
    pass

  def visit_option_string(self, node):
    pass

  def depart_option_string(self, node):
    pass

  def visit_admonition(self, node):
    pass

  def depart_admonition(self, node):
    pass

  def visit_organization(self, node):
    self.visit_docinfo_item(node, 'organization')

  def depart_organization(self, node):
    self.depart_docinfo_item()

  def visit_paragraph(self, node):
    if len(self.nextparagraph):
      self.paragraph.append(self.nextparagraph.pop())
    elif not self.in_pre_document and not self.in_citation:
      self.paragraph.append(True)
      self.begintag(node, 't')
    else:
      self.paragraph.append(False)

  def depart_paragraph(self, node):
    if self.paragraph.pop():
      self.endtag()

  def visit_problematic(self, node):
    pass

  def depart_problematic(self, node):
    pass

  def visit_raw(self, node):
    pass

  def visit_reference(self, node):
    if isinstance(node.parent, nodes.TextElement) and self.in_middle:
      self.begintag(node, 'eref', '', target=node['refuri'])

  def depart_reference(self, node):
    if isinstance(node.parent, nodes.TextElement) and self.in_middle:
      self.endtag()

  def visit_revision(self, node):
    pass

  def depart_revision(self, node):
    pass

  def visit_row(self, node):
    pass

  def depart_row(self, node):
    pass

  def visit_rubric(self, node):
    pass

  def depart_rubric(self, node):
    pass

  def visit_section(self, node):
    if self.in_author:
      self.in_author = False
    if self.in_document_front:
      self.in_document_front = False
      self.endtag()
      self.begintag(node, 'middle')
      self.in_middle = True
    self.section_level += 1
    def add_section(node):
      self.body.append('<section title="%s">' % node.astext())
      self.context.append('</section>')
    self.textcl.append(add_section)

  def depart_section(self, node):
    self.body.append(self.context.pop())
    self.section_level -= 1

  def visit_sidebar(self, node):
    pass

  def depart_sidebar(self, node):
    pass

  def visit_status(self, node):
    self.visit_docinfo_item(node, 'status', meta=None)

  def depart_status(self, node):
    self.depart_docinfo_item()

  def visit_strong(self, node):
    self.begintag(node, 'spanx', '', style='strong')

  def depart_strong(self, node):
    self.endtag()

  def visit_subscript(self, node):
    self.body.append(self.starttag(node, 'sub', ''))

  def depart_subscript(self, node):
    self.body.append('</sub>')

  def visit_substitution_definition(self, node):
    """Internal only."""
    raise nodes.SkipNode

  def visit_substitution_reference(self, node):
    self.unimplemented_visit(node)

  def visit_subtitle(self, node):
    pass

  def depart_subtitle(self, node):
    pass

  def visit_superscript(self, node):
    self.body.append(self.starttag(node, 'sup', ''))

  def depart_superscript(self, node):
    self.body.append('</sup>')

  def visit_system_message(self, node):
    self.body.append(self.starttag(node, 'div', CLASS='system-message'))
    self.body.append('<p class="system-message-title">')
    backref_text = ''
    if len(node['backrefs']):
      backrefs = node['backrefs']
      if len(backrefs) == 1:
        backref_text = ('; <em><a href="#%s">backlink</a></em>'
                        % backrefs[0])
      else:
        i = 1
        backlinks = []
        for backref in backrefs:
          backlinks.append('<a href="#%s">%s</a>' % (backref, i))
          i += 1
        backref_text = ('; <em>backlinks: %s</em>'
                        % ', '.join(backlinks))
    if node.hasattr('line'):
      line = ', line %s' % node['line']
    else:
      line = ''
    self.body.append('System Message: %s/%s '
                     '(<tt class="docutils">%s</tt>%s)%s</p>\n'
                     % (node['type'], node['level'],
                        self.encode(node['source']), line, backref_text))

  def depart_system_message(self, node):
    self.body.append('</div>\n')

  def visit_table(self, node):
    self.body.append(
        self.starttag(node, 'table', CLASS='docutils', border="1"))

  def depart_table(self, node):
    self.body.append('</table>\n')

  def visit_target(self, node):
    if not (node.has_key('refuri') or node.has_key('refid')
            or node.has_key('refname')):
      self.body.append(self.starttag(node, 'span', '', CLASS='target'))
      self.context.append('</span>')
    else:
      self.context.append('')

  def depart_target(self, node):
    self.body.append(self.context.pop())

  def visit_tbody(self, node):
    self.write_colspecs()
    self.body.append(self.context.pop()) # '</colgroup>\n' or ''
    self.body.append(self.starttag(node, 'tbody', valign='top'))

  def depart_tbody(self, node):
    self.body.append('</tbody>\n')

  def capture_term(self, node):
    self.begintag(node, "t", hangText=node.astext())

  def visit_term(self, node):
    self.textcl.append(self.capture_term)

  def depart_term(self, node):
    pass

  def visit_tgroup(self, node):
    # Mozilla needs <colgroup>:
    self.body.append(self.starttag(node, 'colgroup'))
    # Appended by thead or tbody:
    self.context.append('</colgroup>\n')
    node.stubs = []

  def depart_tgroup(self, node):
    pass

  def visit_thead(self, node):
    self.write_colspecs()
    self.body.append(self.context.pop()) # '</colgroup>\n'
    # There may or may not be a <thead>; this is for <tbody> to use:
    self.context.append('')
    self.body.append(self.starttag(node, 'thead', valign='bottom'))

  def depart_thead(self, node):
    self.body.append('</thead>\n')

  def visit_title(self, node):
    if isinstance(node.parent, nodes.topic):
        raise nodes.SkipNode
    if isinstance(node.parent, nodes.section):
      raise nodes.SkipDeparture
    self.begintag(node, 'title')

  def depart_title(self, node):
    if isinstance(node.parent, nodes.document):
      self.in_pre_document = True
      self.in_document_front = len(self.body)
    self.endtag()

  def visit_title_reference(self, node):
    pass

  def depart_title_reference(self, node):
    pass

  def visit_topic(self, node):
    if 'abstract' in node['classes']:
      self.begintag(node, 'abstract')

  def depart_topic(self, node):
    self.endtag()

  def visit_transition(self, node):
    pass

  def depart_transition(self, node):
    pass

  def visit_version(self, node):
    self.visit_docinfo_item(node, 'version', meta=None)

  def depart_version(self, node):
    self.depart_docinfo_item()

  def unimplemented_visit(self, node):
    raise NotImplementedError('visiting unimplemented node type: %s'
                              % node.__class__.__name__)


class RFC(writers.Writer):
  def __init__(self):
    writers.Writer.__init__(self)
    self.translator_class = RFCTranslator

  def get_transforms(self):
    return writers.Writer.get_transforms(self) + [writer_aux.Admonitions]

  def translate(self):
    self.visitor = visitor = self.translator_class(self.document)
    self.document.walkabout(visitor)
    self.output = "".join(self.visitor.astext())

description = ('Generates XML2RFC documents from standalone reStructuredText '
               'sources.  ' + default_description)

publish_cmdline(writer=RFC(), description=description)
