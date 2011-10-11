==============================
Google Wave Conversation Model 
==============================

.. Use headers in this order #=~-_

:toc: yes
:symrefs: yes
:sortrefs: yes
:compact: yes
:subcompact: no 
:rfcedstyle: no
:comments: no
:inline: yes 
:private: yes   

:author: Joe Gregorio
:organization: Google, Inc
:contact: jcgregorio@google.com

:author: Alex North 
:organization: Google, Inc
:contact: anorth@google.com

:Abstract:
    Google Wave is a communication and collaboration platform based on hosted
    conversations, called waves. A wave comprises a set of concurrently editable
    structured documents and supports real-time sharing between multiple
    participants. This document is one in a larger set of specifications for Google
    Wave. This specification describes Google Wave conversation manifest document
    schema and blip document schema, which together define the Google Wave
    conversation model. This application model is layered upon the documents,
    operations and operational transformation defined in the Wave Federation
    Protocol; see http://www.waveprotocol.org/draft-protocol-specs.

:date: 2009 Oct


Introduction
############

A wave can be used to represent a conversation. This specification describes
the Google Wave conversation model, which implements the conversation structure
within structured documents in a wave.

Document Status
===============

This document represents work in progress.  It omits details that we
are unable to capture at this point and we expect parts of the
protocol to change.  Please also note that while we revise the
protocol and white papers, some transient inconsistencies will occur.

This document is one in a larger set of specifications for Google
Wave; see `http://www.waveprotocol.org/draft-protocol-specs 
<http://www.waveprotocol.org/draft-protocol-specs>`_.

Applicability
#############
The wave documents described in this specification may, in whole or in part, be
interchanged between servers via the Google Wave Federation Protocol. The
structure within a wave document is reminiscent of XML. A document contains an
XML-like tree structure of elements, with attributes, and text. In addition, a
document includes stand-off key/value annotations over arbitrary ranges.
Documents are subject to constraints expressed in schemas similar to XML
schemas. 

The use of XML in this specification in no way forces the server implementation
to be XML-based. That is, XML is used as a convenient model and implementations
are free to implement that model in the manner that is most convenient and
performant.

Terminology
###########
The capitalized key words "MUST", "MUST NOT",
"REQUIRED", "SHALL", "SHALL NOT", "SHOULD",
"SHOULD NOT", "RECOMMENDED", "MAY", and
"OPTIONAL" in this document are to be
interpreted as described in [RFC2119]_.


Editorial Notes
###############
To provide feedback on this draft join the wave-protocol 
mailing list at
`http://groups.google.com/group/wave-protocol <http://groups.google.com/group/wave-protocol>`_


Roadmap
#######
Wave is being actively developed and some features of the
conversation model have not been documented, or may change in
the short term. Currently undocumented are annotations, RTL
text, images, and form elements.  

Here is a short roadmap of upcoming changes: 

* A mechanism for signalling blip submission  
* A mechanism for signalling relevance of changes  
* Modification timestamps 
* Cursors and selections 
* Attachments 
* Rich text 


Relationship between specifications
###################################
The Federation Protocol specifies the form and function of
operations that mutate wave documents. While this specification
deals only with conversation manifest documents and blip
documents, a wavelet may contain documents of other types.
Wavelets have metadata that is not stored in document form,
including the wave id, wavelet id, and list of named
participants for the wavelet. See the Federation Protocol for
more details on wavelets and participants. 


Terminology
###########
The following terminology is used by this specification: 

* wave - a collection of wavelets 
* wavelet - a collection of named documents and participants, and the domain of operational transformation 
* conversation - an interpretation of a wavelet as a structured collection of messages 
* document - a structured wave document 
* blip - a document containing a conversational message 
* thread - a sequence of blips, each a "continuation" to that before it 
* conversation manifest - a document that defines the structure of a conversation 
* reply - a thread that represents a reply to conversation material appearing above it 
* inline reply - a reply anchored to a particular point in the replied-to blip 
* private reply - a conversation wavelet with a restricted set of participants  


Model
#####

A conversation wave is a forest of conversations. Each
conversation has a set of participants collaborating on a
structured collection of blips. The structure of the
conversation is maintained in the conversation manifest
document, while the content of the conversational messages is
stored in blip documents. Clients participate in the
conversation by sending operations which mutate the
conversation manifest and blips. 

The following assumptions about the wave model and operational
transforms should be kept in mind when reading this
specification. 
                
* A wavelet may contain many documents which may or may
  not be part of the conversation. This specification
  addresses only the two document types needed for the
  conversation model: blip documents and conversation
  manifest documents.  
* Documents not referenced by the conversation manifest
  are data documents and are not part of the conversation
  content.


Document namespace and validation
=================================
Every document in a wavelet has an identifier unique within the wavelet. Ids of
documents are structured as a sequence of '+'-separated tokens. The first token
of the id is conventionally the document namespace. That wavelet and document
namespace determine the type of document (its schema). Different types of
documents may have different validity constraints. Operations which violate the
schema for a document must be rejected by the server. 

The namespaces for the documents described in this specification are:  

b
  Blip document

conversation
  Conversation Manifest document 


An example blip document id::

  b+a8w3SD_k

Documents
=========

This section describes the two document types that make up the conversation
model.As a general principle of data modeling in waves, metadata is embedded in
documents where it can be manipulated by operations. The conversation metadata
includes the message structure and contributors. Below are examples of a blip
document and a conversation manifest document.
                
Blip document example::

  <contributor name="dadams@acmewave.com"></contributor>
  <body> 
      <line></line>There is a theory which states that if
      <line></line>ever anybody discovers exactly what the
      <line></line>Universe is for and why it
      <line></line>is here, it will instantly disappear and
      <line></line>be replaced by something even more bizarre
      <line></line>and inexplicable. There is another theory
      <line></line>which states that this has already happened.  
  </body>
            
Conversation manifest document example::

 <conversation sort="m"> 
     <blip id="b+a"> <!-- first message --> 
         <thread id="3Fsd"> 
             <blip id="b+aaa"></blip> <!-- indented reply to "b+a" --> 
             <blip id="b+aab"></blip> <!-- continuation to "b+aaa" --> 
         </thread> 
         <thread id="jjKs"> 
             <blip id="b+aba"></blip> <!-- another reply to "b+a" --> 
         </thread> 
     </blip> 
     <blip id="b+b"> <!-- continuation after "b+a" --> 
         <peer id="chess+4342"></peer> <!-- consistency peer id --> 
         <thread inline="true" id="J362"> 
             <blip id="b+baa"></blip> <!-- inline reply to "b+b" --> 
             <blip id="b+bab"></blip> <!-- continuation to "b+aaa" --> 
         </thread> 
         <thread inline="true" ... > <!-- another inline reply group, 
             possibly at the same location --> 
             ... 
         </thread> 
         <thread id="9dKx"> <!-- non-inline reply to "b+b" --> 
             ... 
         </thread> 
     </blip> 
 </conversation>


Conversation Manifest Document
==============================

A conversation manifest document has the distinguished document
id 'conversation', and is the only document in the
'conversation' namespace. There is one conversation manifest
document per conversation.

The conversation manifest defines the logical structure of the
conversation by describing the relationships between the blips.
See the Display section for how the logical structure of
conversations is reflected in the user-interface of a wave
client.

Blip elements are wrapped in a grouping element 'thread',
except for top-level blips which belong to an implicit root
thread. A blip may have multiple child 'thread' elements
representing replies. Each 'thread' element has an id, unique
within the conversation. Thread ids have no semantic meaning.

Here is a very simple conversation manifest that references only a single blip. ::

 <conversation>
     <blip id="b+a"> <!-- first and only message -->
     </blip>
 </conversation>

Here is a more complex conversation manifest that references multiple blips. ::

 <conversation>
     <blip id="b+a"> <!-- first message -->
         <thread id="x123">
             <blip id="b+aaa"></blip> <!-- indented reply to "b+a" -->
         </thread>
         <thread inline="true" id="x983">
             <blip id="b+aba"></blip> <!-- another reply to "b+a" -->
             <blip id="b+abb"></blip> <!-- another reply to "b+a" -->
         </thread>
     </blip>
 </conversation> 



Elements
~~~~~~~~
These are the allowed elements in a conversation manifest.

conversation 
------------
The top-level element in a conversation manifest document.
It may have anchor attributes ("anchorWavelet",
"anchorManifestOffset", "anchorVersion", "anchorBlip",
"anchorOffset"), which determine where this conversation is
displayed. See the section below on private replies. 
It also has a "sort" attribute. The value of the sort attribute is used to
determine the order of peer conversation elements by sorting on sort values
lexicographically.  A conversation element has zero or more 'blip' elements as
children.  

anchorWavelet
    The id of the wavelet that this conversation is a reply to.

anchorManifestOffset
    An integer offset into the document sequence for the parent's conversation manifest. 

anchorVersion
    The version of the parent wavelet when the private reply was created.

anchorBlip
    The id of the blip document in the parent wavelet that anchors the reply.

anchorOffset
    An integer offset into the replied-to blip document that anchors the private reply.

blip 
------------
Represents a message. Every blip element has an 'id'
attribute that references a blip document in the
conversation. It may also have a 'deleted' attribute whose
boolean value determines if that blip has been logically
deleted. See the section below on deleting blips.
A blip may have zero or more 'thread' and 'peer' elements
as children. 

thread 
------------
Represents a sequence of messages. All sibling blips in a thread element are
considered a reply to the parent blip element.  A thread element has an
"id" and optional boolean "inline" attribute. The "inline" attribute determines
whether the thread is anchored inline in the parent blip. The default value
for "inline" if false, that is, the inline attribute not being present is
the same thing as inline=false. A thread may have zero or more 'blip' elements as children. 

peer 
------------
A peer element has an "id" attribute. The "id" attribute value is the id of a
data document. The identified data document is not a blip or conversation
manifest document. 


Private Replies
~~~~~~~~~~~~~~~
Private replies are represented as distinct wavelets with their
own conversation manifest containing a reference to the parent
conversation wavelet. Sub-conversations reference the parent
conversation's manifest document with a (wavelet-id, blip-id,
location, version) tuple referred to as an anchor. This tuple
refers to a parent blip by naming the replied-to wavelet and
blip. The location and version attributes refer
to the corresponding blip element in the replied-to
conversation's manifest document at some previous version. This
ensures the information is still present if the parent blip is
deleted. 

An inline private reply also references an anchor point in the
replied-to blip at a selected version. A forthcoming mechanism
will allow clients to request a server to transform a location
to the current version for rendering.

This representation prevents leakage of the existence of the
private reply to participants who cannot access it. Note that
this structure still leaks the existence of the parent
conversation to the private reply. 

The anchoring tuple is represented as a set of optional attributes on the
conversation tag of the conversation manifest document. 

Examples
~~~~~~~~

A root wavelet has no anchoring information. ::

  <conversation></conversation> 


A non-inline private reply wavelet, referencing a blip in the
conversation manifest document. The sort attribute determines
sibling wavelet sort order by lexicographic value order.  ::

  <conversation
     sort="p"
     anchorWavelet="conv+root"
     anchorBlip="b+123"
     anchorManifestOffset="45"
     anchorVersion="7436" >                        
  </conversation> 

An inline private reply, also has an anchor offset
referring to the replied-to blip.  ::

  <conversation
     sort="p"
     anchorWavelet="conv+root"
     anchorBlip="b+123"
     anchorManifestOffset="45"
     anchorVersion="7436"
     anchorOffset="784"> 
  </conversation> 


Blip Document
=============

A blip document is distinguished by having a document id
that begins with the namespace 'b'.

A simple blip document. ::

    <contributor name="me@gwave.com"></contributor>
    <contributor name="you@gwave.com"></contributor>
    <body>
        <line></line>The quick brown fox...
    </body>

A more complex blip document. ::

    <contributor name="me@gwave.com"></contributor>
    <contributor name="you@gwave.com"></contributor>
    <contributor name="fred@acmewave.com"></contributor>
    <body>
        <line></line>The quick brown fox
        <line></line>Jumped over
        <line></line>the lazy dog.
        <image attachment="a+sadkfd">
            <caption>A lazy dog.</caption>
        </image>
    </body> 

Schema Design
~~~~~~~~~~~~~
The documents representing messages (blips) conform to a very simple schema. 

The blip document schema expresses a structured representation of the message
with little presentation logic, and is mostly not web-specific. For example,
most rich-text styling is represented in annotations. This representation
behaves in a much more natural way when two clients concurrently edit
overlapping regions of text. The document representation may not correspond to
the most natural semantic interpretation, but is designed to behave most
naturally under operational transformation. 

Elements
~~~~~~~~

The allowed elements of a blip are:

contributor
-----------
Each contributor element has a single
required attribute 'name' with the id of a participant who
has contributed to the blip content. If there are duplicates
(which may occur with concurrent editing) then the first is
the canonical contributor and the others should be ignored or
removed. Individual contributors are responsible for adding themselves
to this list. This allows for "trivial" contributors such as
annotators to voluntarily omit themselves. Absolute
contributor information may be recovered from the operation
history if required.  

body
----
The displayed content of the blip. The body element may
contain text and 'line' elements. The first child of the body
element must be a line element. If a document contains two
body elements then the first is the canonical body and others
should be ignored or removed. 

line
----
The displayed textual content of a blip
is broken up into lines. Each line is preceded with a "line"
element. The line element must be an empty element, that is,
having no other items between the begin element and the end
element. The line element may have the following attributes:

t
    The type of the line. Allowed values
    are 'h1', 'h2', 'h3', 'h4', 'h5', and 'li', where h[1-5] is a
    heading and 'li' an item in a list. 

i
    The indentation level (0,1,2,...). This attribute is only
    meaningful when applied to lines of type t="li". 

a
    The alignment of the text in the line. (a, m, r, j)
    a = align left = centered = alighted right = justified 

d
    The display direction of the line
    l = left to right, r = right to left 

image
-----
The image element represents an attached image embedded in
the blip. The image element has an 'attachment' attribute
that is the id of that attachment's data document.
Attachments and attachment documents are described in the
Google Wave Attachments whitepaper. 

reply 
-----
The reply element denotes the location of an inline reply
thread. It has an attribute 'id' that contains the id of the
thread whose location it marks. Thus an inline reply's inline
location is marked with a reply element. 

Annotations
~~~~~~~~~~~
The following are the allowed annotations allowed in blip
documents. 

Style
-----
Style annotations control the display of the content in the
blip. All style annotations have keys that begin with
"style/". The allowed values for the style annotations are
the same as those of the CSS properties of the same name. 

* style/backgroundColor
* style/color
* style/fontFamily
* style/fontSize
* style/fontStyle
* style/fontWeight
* style/textDecoration
* style/verticalAlign  

User
----
The following annotations refer to a user id
and a session id. Each client gets its session
id from the server. Session ids, their
assignment, and how they are transmitted to a
client are out of scope for this document. It
should be noted that to avoid name clashes when
federating waves the session id should include
the domain of the server generating them.
Style annotations control the display of the
content in the blip.  

User annotations contain information that is
specific to each user session. All user
annotation keys begin with 'user/'.  

                                
user/d/&lt;session id>
    This annotation covers the entire
    document. The value of the
    annotation is a comma separated
    list of (userid, timestamp [,ime
    composition state]) The timestamp
    is the last time the cursor was
    updated. The timestamp may be used
    by clients to stop dislpaying the
    users carat after a period of
    inactivity.  

user/r/&lt;session id>
    This annotation represents the
    users selection. That is, the range
    of text with this annotation is
    text that the user has selected. If
    the user does not have any text
    selected then this annotation is
    not present. Note that the
    currently implementation only
    supports a single selection region
    per user. The value of this
    annotation is the user id. 

user/e/&lt;session id>
    This annotation represents the
    user's selection focus (the "blinky
    bit"). The first point in the range
    of the annotation is the cursor
    location for the users session.
    That is, the cursor is placed
    before the first item in the
    annotation range. This annotation
    always extends from the cursor
    position to the end of the
    document. If this annotation is
    missing then the cursor is placed
    after the last item in the
    document. The value of this
    annotation is the user id.  

Links
-----
Link annotations define links to
other resources. All link annotations have keys that begin
with "link/". 

link/manual
    A manually created link. A URI or IRI? is the only valid
    value for this annotation. 

link/auto
    A link created automatically by some linkifying process. Such annotations have a
    lower precedence than manual links. A URI is the only valid
    value for this annotation. 

link/wave
    A link to another wave. Wave ids are the only valid values
    for this annotation.  

Example Conversations
#####################

Simple Replies
==============
    
This is an example conversation showing how a conversation is
represented by this model. This conversation consists of two
blips and a conversation manifest document in one wavelet. 

The conversation manifest has an id of "conversation" and is::

    <conversation>
        <blip id="b+a">
            <thread id="r1">
                <blip id="b+b"></blip>
                <blip id="b+c"></blip> 
            </thread> 
        </blip>
    </conversation>

There is a blip with an id of "b+a"::

    <contributor name="fred@acmewave.com"></contributor>
    <body> 
        <line></line>There is a theory which states 
        <line></line>that if ever anybody
        <line></line>discovers exactly what the 
        <line></line>Universe is for and why it
        <line></line>is here, it will instantly 
        <line></line>disappear and be replaced by 
        <line></line>something even more bizarre 
        <line></line>and inexplicable. There is another
        <line></line>theory which states that this 
        <line></line>has already happened. 
    </body>

A reply blip with an id of "b+b"::

    <contributor name="barney@acmewave.com"></contributor>  
    <body> 
        <line></line>Isn't that a quote from Douglas Adams? 
    </body> 

And a reply blip with an id of "b+c"::

    <contributor name="fred@acmewave.com"></contributor>   
    <body> 
        <line></line>Yes it is. 
    </body> 


In-line Replies
~~~~~~~~~~~~~~~
The above shows the conversation with non-inline replies.
Here is the same conversation, but the replies are in-line.
This conversation will display differently from the above
conversation.  

The conversation manifest has an id of "conversation" and is::

    <conversation>
        <blip id="b+a">
            <thread inline="true" id="r1">
                <blip id="b+b"></blip>
                <blip id="b+c"></blip> 
            </thread> 
        </blip>
    </conversation>

There is a blip with an id of "b+a"::

    <contributor name="fred@acmewave.com"></contributor>
    <body> 
        <line></line>There is a theory which states 
        <line></line>that if ever<reply id='aF8j_s'></reply> anybody
        <line></line>discovers exactly what the 
        <line></line>Universe is for and why it
        <line></line>is here, it will instantly 
        <line></line>disappear and be replaced by 
        <line></line>something even more bizarre 
        <line></line>and inexplicable. There is another
        <line></line>theory which states that this 
        <line></line>has already happened. 
    </body>

Note the addition of the reply element which
anchors the in-line reply. 

A reply blip with an id of "b+b"::

    <contributor name="barney@acmewave.com"></contributor>   
    <body> 
        <line></line>Isn't that a quote from Douglas Adams? 
    </body> 

And a reply blip with an id of "b+c"::

    <contributor name="fred@acmewave.com"></contributor>   
    <body> 
        <line></line>Yes it is. 
    </body> 

Private In-line Replies
-----------------------
The above shows the conversation with inline replies. Here is the same
conversation, but the replies are private in-line. This conversation will
display differently from the above two conversations.  

The conversation manifest has an id of "conversation" and
is an contained in the wavelet with an id of 'wave+a'. ::

    <conversation>
        <blip id="b+a"></blip>
    </conversation>

There is a blip with an id of "b+a"::

    <contributor name="fred@acmewave.com"></contributor>
    <body> 
        <line></line>There is a theory which states 
        <line></line>that if ever anybody
        <line></line>discovers exactly what the 
        <line></line>Universe is for and why it
        <line></line>is here, it will instantly 
        <line></line>disappear and be replaced by 
        <line></line>something even more bizarre 
        <line></line>and inexplicable. There is another
        <line></line>theory which states that this 
        <line></line>has already happened. 
    </body>
    
The replies are contained in another wavelet with an id of 'wave+b'.

Being a wavelet it has its own conversation manifest::

    <conversation
        sort="r"
        anchorWavelet="wave+a"
        anchorBlip="b+a"
        anchorManifestOffset="1"
        anchorVersion="12"
        anchorOffset="10">
       <blip id="b+b"></blip>
       <blip id="b+c"></blip> 
    </conversation> 

The reply blips are in the 'wave+b' wavelet. There is the reply blip with an id of "b+b"::

    <contributor name="barney@acmewave.com"></contributor>   
    <body> 
        <line></line>Isn't that a quote from Douglas Adams? 
    </body> 

And a reply blip with an id of "b+c"::

    <contributor name="fred@acmewave.com"></contributor>   
    <body> 
        <line></line>Yes it is. 
    </body> 


Display
#######
The structure and relationships between wavelets, conversation manifests
and blips defines a logical structure for conversations. What follows is a
description of how the content and structure defined is presented in a
user-interface, fully realizing that implementing wave on different clients
will impose different constraints. The below isn't meant to constrain client
implementations, but to give guidance on providing a consistent user
experience.   

A conversation should be displayed as a single unit and
may reference more than one wavelet. A wavelet is
considered to be in a conversation view if it has a wavelet
id in the "conv" namespace and the user has permission to
access the wavelet. Wavelets other than the root in a
conversation view should reference another via an
anchorWavelet attribute.   

Blips are displayed in the order that they
appear in the conversation manifest, going from top to bottom. Blip documents
in a wavelet that are not referenced in the conversation manifest should not be
displayed. Threads are indented from the content of their parent blip unless
they have a property value of inline=true. The location of private replies is
determined by the "anchorWavelet", "anchorManifestOffset", "anchorVersion",
"anchorBlip", and "anchorOffset" properties of the conversation element. Note
that the anchor position is given for version at which the private reply was
created, and is recorded in the "anchorVersion" attribute. The proper display
location for the private reply will depend upon keeping track of how that
historical position moves as documents are mutated.   

The location of
non-private inline replies is denoted with a reply element in the thread and
should be displayed indented at that location.   

The display of private
inline replies should include an indication that it is private, such as
displaying the participants for that wavelet, along with the content of the
sub-conversation.  


Mechanisms
##########
This section describes some mechanisms for changing the conversation
structure. Additional mechanisms remain to be defined. In addition, the
interactions of concurrent modifications have yet to be detailed. 

Creating a new blip
===================
To add a new blip to a thread an operation is sent to
insert a blip element (with a unique id) to the thread in the conversation
manifest. The added blip document may or may not be empty

Creating a reply thread
=======================
To add a new thread an operation is sent to insert a thread
element (with a unique id) to the replied-to blip element in the conversation
manifest. If the thread is an inline reply an anchor element must first be
inserted in the replied to blip. 

Deleting a blip
===============
Deleting a blip involves deleting the blip, any inline replies to the blip, and
(optionally) any non-inline replies to the blip. This mechanism deletes only
inline replies.  

A blip is deleted by sending mutations that: 

* Delete all subordinate inline threads by: 
  - Deleting all blips in the thread 
  - Removing the thread element from the conversation manifest  

* Transform the blip's document to an empty document 
* Set the "deleted" attribute value to 'true' on the blip's conversation
  manifest element if the blip has non-inline replies, else deletes the element
  entirely. 

After a blip is deleted, non-inline replies to that blip are still nested
within their parent in the conversation manifest. They may be rendered as
children of a deleted blip. Future restructure operations may allow the
children to be re-parented. If a deleted blip has no remaining replies then the
blip entry should be deleted from the conversation manifest document. If the
deleted blip was the last in a thread then the thread entry should be removed
from the manifest document.  

Edits that occur concurrently with a blip
deletion are nullified in transform. Concurrently created replies to a deleted
blip are deleted if the blip has no replies, but survive if the blip element
remains (with "deleted" set to "true").  

References
##########

.. [RFC2119] Bradner, S., "Key words for use in RFCs to Indicate Requirement Levels", BCP 14, RFC 2119, March 1997.
