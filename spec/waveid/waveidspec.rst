=======================
Google Wave Identifiers
=======================

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

:author: Soren Lassen
:organization: Google, Inc
:contact: soren@google.com

:author: Alex North
:organization: Google, Inc
:contact: anorth@google.com

:Abstract:
    Google Wave is a communication and collaboration platform based on hosted
    conversations, called waves. A wave comprises a set of concurrently editable
    structured documents and supports real-time sharing between multiple
    participants. This document is one in a larger set of specifications for Google
    Wave. This specification describes Google Wave identifiers, which are used
    to identify waves, wavelets, and documents.

:date: 2010 Mar


Introduction
############

Document Status
===============

This document represents work in progress.  It omits details that we
are unable to capture at this point and we expect parts of the
protocol to change.  Please also note that while we revise the
protocol and white papers, some transient inconsistencies will occur.

This document is one in a larger set of specifications for Google
Wave; see `http://www.waveprotocol.org/draft-protocol-specs 
<http://www.waveprotocol.org/draft-protocol-specs>`_.


Terminology
===========
The capitalized key words "MUST", "MUST NOT",
"REQUIRED", "SHALL", "SHALL NOT", "SHOULD",
"SHOULD NOT", "RECOMMENDED", "MAY", and
"OPTIONAL" in this document are to be
interpreted as described in [RFC2119]_.

This specification uses the Augmented Backus-Naur Form (ABNF)
notation of [RFC5234]_. The following ABNF rules are imported from
the normative references [RFC5234]_, [RFC3986]_, and [RFC3987]_.::

     pct-encoded    =  "%" HEXDIG HEXDIG
     unreserved     =  ALPHA / DIGIT / "-" / "." / "_" / "~"
     reserved       =  gen-delims / sub-delims
     gen-delims     =  ":" / "/" / "?" / "#" / "[" / "]" / "@"
     sub-delims     =  "!" / "$" / "&" / "'" / "(" / ")"
                    /  "*" / "+" / "," / ";" / "="

     iunreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~" / ucschar
     ucschar        =  %xA0-D7FF / %xF900-FDCF / %xFDF0-FFEF
                    /  %x10000-1FFFD / %x20000-2FFFD / %x30000-3FFFD
                    /  %x40000-4FFFD / %x50000-5FFFD / %x60000-6FFFD
                    /  %x70000-7FFFD / %x80000-8FFFD / %x90000-9FFFD
                    /  %xA0000-AFFFD / %xB0000-BFFFD / %xC0000-CFFFD
                    /  %xD0000-DFFFD / %xE1000-EFFFD
     ireg-name      = *( iunreserved / pct-encoded / sub-delims )



Editorial Notes
===============
To provide feedback on this draft join the wave-protocol 
mailing list at
`http://groups.google.com/group/wave-protocol <http://groups.google.com/group/wave-protocol>`_

Overview
########

A wave identifier provides a simple and extensible means for identifying parts
of a wave. This goal of this specification and syntax for wave identifiers is
to construct identifiers that may be used as parts of a URI or as a URL query
parameter without excessive escaping so that a URI path form is the canonical
serialisation. In addition, the syntax is designed to accomodate
internationalized identifiers.

In a wave implementation a wave identifier appears on the wire in the
federation and client/server protocols, and in  addition may be serialized to
disk. Wave identifiers may be included in the path or query of an HTTP URI, and
should be structured to meet the conflicting goals of uniqueness and clarity
when serialized into such URIs.::

  http://example.com/waveref/<waveid>

  http://example.com/waveref?id=<waveid>

  http://example.com/waveref#<waveid>

Note that this draft specification has references to particular documents,
offsets, and versions in mind, but does not specify them explicitly at this
time.

This specification restricts the allowed character set in a wave id to aid
construction of clean and readable URI-references.

Identifier Syntax
#################

A wave identifier is a domain name and a local identifier joined by '/'.

The domain name is a unicode string, and may contain international alphanumeric
characters, as described by the IDNA standard [RFC3490]_. Future version of this 
specification will detail serialization, which depends on context.

The local identifier is a non-empty unicode string drawn from the character set::

  lchar       = iunreserved / "+" / "*" / "@"
  localid     = 1*lchar
  waveid      = ireg-name "/" localid

A wavelet identifier is a domain name and a local identifier joined by '/'.::

  waveletid = ireg-name "/" localid

A wavelet name is an identifier for a wavelet that includes the wave
identifier and the wavelet identifier. It is constructed by concatenating the
two identifiers with a "/"::

  wavename    = waveid "/" waveletid

Identifier Constraints
######################

The constructed wave and wavelet identifiers have differing uniqueness 
constraints which are described below.

Local Identifier
================

Local identifiers may be structured by an application, but that structure is
not specified here.

Wave Identifier
===============
For wave identifiers that identify waves, the domain and localid of a wave
identifier together as a pair MUST be globally unique.

Example::

  example.com/w+2cDs_sd

Wavelet Identifier
==================
For identifiers the identify wavelets, the domain and localid of a wavelet
identifier together MUST be unique within a wave.

Examples::

  tūdaliņ.lv/user+töm@tūdaliņ.lv

  example.com/prof+hügo@example.com

  tūdaliņ.lv/robot+tweety.appspot.com

Wavelet Name
============
Wavelet names are constructed from wave identifiers and wavelet identifiers
that meet their individual constraints, and are thus by construction globally unique.

Example::

  example.com/w+2cDs_sd/tūdaliņ.lv/user+töm@tūdaliņ.lv


References
##########
.. [RFC2119] Bradner, S., "Key words for use in RFCs to Indicate Requirement Levels", BCP 14, RFC 2119, March 1997.
.. [RFC3986] Berners-Lee, T., Fielding, R., and L. Masinter, "Uniform Resource Identifier (URI): Generic Syntax", STD 66, RFC 3986, January 2005.
.. [RFC3987] Duerst, M. and M. Suignard, "Internationalized Resource Identifiers (IRIs)", RFC 3987, January 2005.
.. [RFC5234] Crocker, D. and P. Overell, "Augmented BNF for Syntax Specifications: ABNF", STD 68, RFC 5234, January 2008.
.. [RFC3490] Faltstrom, P., Hoffman, P. and Costello, A., "Internationalizing Domain Names in Applications (IDNA)", RFC 3490, March 2003
