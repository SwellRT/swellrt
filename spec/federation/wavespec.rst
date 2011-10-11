=========================================
Google Wave Federation Protocol Over XMPP 
=========================================

.. Use headers in this order #=~-_

:toc: yes
:symrefs: yes
:sortrefs: yes
:compact: yes
:subcompact: no 
:rfcedstyle: no
:comments: no
:inline: yes 
:private: .
  
:author: Anthony Baxter 
:organization: Google, Inc
:contact: arb@google.com
 
:author: Jochen Bekmann 
:organization: Google, Inc
:contact: jochen@google.com

:author: Daniel Berlin 
:organization: Google, Inc
:contact: dannyb@google.com

:author: Joe Gregorio
:organization: Google, Inc
:contact: jcgregorio@google.com

:author: Soren Lassen
:organization: Google, Inc
:contact: soren@google.com

:author: Sam Thorogood
:organization: Google, Inc
:contact: thorogood@google.com

:Abstract:
    Google Wave is a communication and collaboration platform based on hosted
    conversations, called waves. A wave comprises a set of concurrently editable
    structured documents and supports real-time sharing between multiple
    participants. This specification describes the Google Wave Federation
    Protocol Over XMPP, an open extension to the XMPP core [RFC3920]_ protocol
    allowing near real-time communication of wave updates between two wave servers.


:date: 2009 July 

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

Overview
========
The Google Wave Federation Protocol Over XMPP is an open extension to
the XMPP core [RFC3920]_ protocol allowing near real-time
communication of wave updates between two wave servers.

Terminology
===========
The capitalized key words "MUST", "MUST NOT",
"REQUIRED", "SHALL", "SHALL NOT", "SHOULD",
"SHOULD NOT", "RECOMMENDED", "MAY", and
"OPTIONAL" in this document are to be
interpreted as described in [RFC2119]_.


Editorial Notes
===============
To provide feedback on this draft join the wave-protocol 
mailing list at
`http://groups.google.com/group/wave-protocol <http://groups.google.com/group/wave-protocol>`_

Architectural Overview
######################

Google Wave is a communication and collaboration platform based on
hosted conversations, called waves.  A wave consists of XML documents
and supports concurrent modifications and low-latency updates between
participants on the wave. 



Relationship between specifications
===================================
The Federation Protocol specifies the form and function of operations that
mutate wave documents. See the Google Wave Conversation Model specification of
conversation manifest documents and blip documents.


Wave Providers
==============

The wave federation protocol enables everyone to become a wave
provider and share waves with others.  For instance, an organization
can operate as a wave provider for its members, an individual can run
a wave server as a wave provider for a single user or family members,
and an Internet service provider can run a wave service as another
Internet service for its users as a supplement to email, IM, ftp,
etc.

A wave provider is identified by its Internet domain name(s).

Wave users have wave addresses which consist of a user name and a
wave provider domain in the same form as an email address, namely
<username>@<domain>.  Wave addresses can also refer to groups,
robots, gateways, and other services.  A group address refers to a
collection of wave addresses, much like an email mailing list.  A
robot can be a translation robot or a chess game robot.  A gateway
translates between waves and other communication and sharing
protocols such as email and IM.  In the remainder we ignore
addressees that are services, including robots and gateways - they
are treated largely the same as users with respect to federation.

Wave users access all waves through their wave provider.  If a wave
has participants from different wave providers, their wave providers
all maintain a copy of the wave and serve it to their users on the
wave.  The wave providers share updates to the wave with each other
using the wave federation protocol which we describe below.  For any
given wave user, it is the responsibility of the wave provider for
the user's domain to authenticate the user (using cookies and
passwords, etc) and perform local access control.

Wavelets
========

A wave consists of a set of wavelets.  When a user has access to a
wavelet, that user is called a participant of that wavelet.  Each
wavelet has a list of participants, and a set of documents that make
up its contents.  Different wavelets of a wave can have different
lists of participants.  Copies of a wavelet are shared across all of
the wave providers that have at least one participant in that
wavelet.  Amongst these wave providers, there is a designated wave
provider that has the definitive copy of that wavelet.  We say that
this particular provider is hosting that wavelet.

When a user opens a wave, a view of the wave is retrieved, namely the
set of wavelets in the wave that the user is a participant of
(directly, or indirectly via group membership).  In general,
different users have different wave views for a given wave.  For
example, per-user data for a user in a wave, such as the user's read/
unread state for the wave, is stored in a user-data wavelet in the
wave with the user as the only participant.  The user-data wavelet
only appears in this user's wave view.  Another example is a private
reply within a wave, which is represented as a wavelet with a
restricted participant list.  The private reply wavelet is only in
the wave views of the restricted list of users.

A wave is identified by a globally unique wave id, which is a pair of
a domain name and an id string.  The domain names the wave provider
where the wave originated.

A wavelet has a wavelet id which is unique within its wave.  Like a
wave id, a wavelet id is a pair of a domain name and an id string.
The domain name in the wavelet id plays a special role: It names the
wave provider that hosts the wavelet.  A wavelet is hosted by the
wave provider of the participant who creates the wavelet.  The wave
provider who hosts a wavelet is responsible both for operational
transformation and application of wavelet operations to the wavelet
and for sharing the applied operations with the wave providers of all
the wavelet participants

Wavelets in the same wave can be hosted by different wave providers.
For example, a user-data wavelet is always hosted by the user's wave
provider, regardless of where the rest of the wave is hosted.
Indeed, user-data is not federated, i.e., not shared with other wave
providers.  Another example is a private reply wavelet.  A
particularly simple instance of this is when all the participants of
the private reply are from the same wave provider.  Then this wave
provider will not share the private reply wavelet with other wave
providers, regardless of where the other wavelets in the wave are
hosted.

Documents
=========

Each wavelet is a container for any number of documents.  Each
document has an id that is unique within its containing wavelet.  It
is composed of an XML document and a set of annotations.  Annotations
are key-value pairs that span arbitrary ranges of the XML document
and are independent of the XML document structure.  They are used to
represent text formatting, spelling suggestions and hyper-links.

Some documents represent rich-text messages in the wavelet.  These
are known as "blips".  The blips in a wave form a threaded
conversation.  Others represent data, for example tags, and are not
displayed to the user as part of the threaded conversation structure
in the wave.  For detailed information on the structure of documents,
please refer to the Google Wave Operational Transformation [OT]_ white
paper.

Operations
==========

Operations are mutations on wavelets.  The state of a wavelet is
entirely defined by a sequence of operations on that wavelet.

Clients and servers exchange operations in order to communicate
modifications to a wavelet.  Operations propagate through the system
to all clients and servers interested in that wavelet.  They each
apply the operation to their own copy of the wavelet.  The use of
operational transformation (OT) guarantees all copies of the
wavelet will eventually converge to the same state.  In order for the
guarantees made by OT to hold, all communication participants must
use the same operational transformation and composition algorithms
(i.e. all OT implementations must be functionally equivalent).

Wave Service Architecture
=========================

A wave provider operates a wave service on one or more networked
servers.  The central pieces of the wave service is the wave store,
which stores wavelet operations, and the wave server, which resolves
wavelet operations by operational transformation and writes and reads
wavelet operations to and from the wave store.  Typically, the wave
service serves waves to users of the wave provider which connect to
the wave service frontend (see Google Wave Data Model and Client-
Server Protocol), and we shall assume this in the following
description of the wave service architecture.  More importantly, for
the purpose of federation, the wave service shares waves with
participants from other providers by communicating with these wave
provider's servers.

For a given wave provider, its wave server serves wave views to local
participants, i.e., participants from its domain.  This wave server
stores the state of all wavelets that it knows about.  Some are
hosted by the wave server itself.  These are "local wavelets"
relative to this wave server.  Others are copies of wavelets hosted
by other wave providers.  These are "remote".  A wave view can
contain both types of wavelet simultaneously.

At a particular wave provider, local wavelets are those created at
that provider, namely by users who belong to the wavelet provider.
The wave server is responsible for processing the wavelet operations
submitted to the wavelet by local participants and by remote
participants from other wave providers.  The wave server performs
concurrency control by ordering the submitted wavelet operations
relative to each other using operational transformation.  It also
validates the operations before applying them to a local wavelet.

Remote wavelets are hosted by other wave providers.  The wave server
maintains cached copies locally and updates them with wavelet
operations that it gets from the hosting wave providers.  When a
local participant submits a wavelet operation to a remote wavelet,
the wave server forwards the operation to the wave server of the
hosting provider.  When the transformed and applied operation is
echoed back, it is applied to the cached copy.  Read access to local
participants is done from the cached copy without a round trip to the
hosting wave provider.

Local and remote wavelets are all stored in the wave server's
persistent wave store.

We say that a wave provider is "upstream" relative to its local
wavelets and that it is "downstream" relative to its remote wavelets.


Federation Host and Federation Remote
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The wave service uses two components for peering with other wave
providers, a "federation host" and a "federation remote".  (In an
earlier revision of this draft specification these components were
called "federation gateway" and "federation proxy", respectively).

The federation host communicates local wavelet operations, i.e.,
operations on local wavelets:

* wavelet to the wave providers of any remote participants.
* It satisfies requests for old wavelet operations.
* It processes wavelet operations submission requests.

The federation remote communicates remote wavelet operations and is
the component of a wave provider that communicates with the
federation host of upstream wave providers:

*  It receives new wavelet operations pushed to it from the wave
   providers that host the wavelets.
*  It requests old wavelet operations from the hosting wave
   providers.
*  It submits wavelet operations to the hosting wave providers.

An upstream wave provider's federation host connects to a downstream
wave provider's federation remote to push wavelet operations that are
hosted by the upstream wave provider.

The federation protocol has the following mechanisms to make
operation delivery from host to remote reliable.  The federation host
maintains (in persistent storage) a queue of outgoing operations for
each remote domain.  Operations are queued until their receipt is
acknowledged by the receiving federation remote.  The federation host
will continually attempt to establish a connection and reconnect
after any connection failures (retrying with an exponential backoff).
When a connection is established, the federation host will send
queued operations.  The receiving federation remote sends
acknowledgements back to the sending federation host and whenever an
acknowledgement is received, the sender dequeues the acknowledged
operations.


Protocol Specification
######################

Connection Initiation and Lifetime
==================================

As an XMPP extension, this protocol expects a bidirectional stream to
be established according to the XMPP core specification.

The connection MUST be secured using the TLS feature of XMPP.  It is
RECOMMENDED that communication is encrypted (namely by using a non-
identity TLS cipher).

All communication except wavelet updates are sent via PubSub
[XEP0060]_ events.  Wavelet updates are sent using Message stanzas.

Cryptographic Certificates and Signatures
==========================================

In the section below there are references to cryptographic signatures
and certificates used to generate them.

The paper by Kissner and Laurie, General Verifiable Federation [VERFED]_
gives a detailed explanation of how we expect to make all changes to
wavelets attributable to their originating servers and render the
federation protocol immune to a number of attacks.  The techniques
described in the paper have not yet been fully implemented or
incorporated into this protocol specification, however certificates
are exchanged using the get signer and post signer XMPP messages.

Stanzas
=======

The federation protocol involves two parties: a wave federation host
and wave federation remote as described above.  The
top level stanzas are divided into two types: those that are part of
the "update stanzas", and those part of the "service stanzas".  The
update stanzas are initiated by a federation host to
a federation remote and carry <update/>s from the
host to the remote.  The service stanzas are
initiated by the federation remote to the federation host and carry
<submit-request/>s and <submit-response/>s, <history-request/>s and
<history-response/>s, <signer-get-request/>s and
<signer-get-response/>s, <signer-post-request/>s and <signer-post-
response/>s.


Commonly used attributes
~~~~~~~~~~~~~~~~~~~~~~~~

These stanzas commonly contain the following attributes:

wavelet-name
------------

The 'wavelet-name' attribute is an encoded form of the following
components:

* A "wave id" specifies the domain of the wave provider that
  originally started the wave, plus an identifier unique within that
  domain. The identifier starts with "w+" by convention.
* A "wavelet id" specifies the domain of the wave provider that
  hosts the wavelet, plus a unique identifier which is unique within
  all wavelets with that domain. The identifier starts with "conv+"
  by convention. The first wavelet added to a wave has the identifier
  "conv+root" by convention.
  
These components are encoded into a single string in the format of a
an absolute URI.  The wavelet id domain is used as the host part
(since this is where the wavelet is hosted).  The wave id is used as
the first path element, which contains the wave id domain if it does
not match the wavelet id domain, in this case it is prepended to a
unique identifier with a '$' delimiter.  The unique identifier in the
wavelet id is the final path element.  URI generic delimiter
characters (:/?#[]@) appearing in the id parts must be percent-
escaped.

For example, a 'wavelet-name' might be "wave://initech-corp.com/
acmewave.com$w+4Kl2/conv+3sG7", where the wavelet id has domain
"initech-corp.com" and unique identifier "conv+3sG7", and the wave id
has domain "acmewave.com" and unique identifier "w+4Kl2".

If the wavelet was hosted at "initech-corp.com" and the wave had also
been started on that domain, the 'wavelet-name' would be "wave://initech-
corp.com/w+4Kl2/conv+3sG7".

Commonly used elements
~~~~~~~~~~~~~~~~~~~~~~
hashed-version
--------------

A <hashed-version/> element contains the version and history hash
pair of a wavelet.

* 'version' -- REQUIRED attribute which contains the version of the
  wavelet. The value of this attribute is a 64-bit integer to base 10.
* 'history-hash' -- REQUIRED attribute which is the value of the
  rolling history hash at the given version. The history-hash attribute is
  a base64 encoding of the rolling hash.

The rolling hash is computed as follows:

* The initial hash value H(0) is the wavelet-name.
* The hash value H(n) is computed as SHA256( H(n-1) + delta )[0..20].

The '+' operator means that the hash and the delta are concatenated.
The delta is a protocol buffer encoding of the message type
protocol::ProtocolAppliedWaveletDelta.

commit-notice
-------------

The <commit-notice/> element is a variant of the <hashed-version/>
element.  It is used to indicate that the wave server has committed
deltas up to this point.

* 'version' -- REQUIRED attribute which contains the version of
  the wavelet. The value of this attribute is a 64-bit integer
  to base 10.

delta
-----

The <delta/> element contains a sequence of one or more operations
grouped for communication to and between wave servers:

* 'wavelet-name' -- REQUIRED wavelet-name (Section 3.3.1.1).
* <operation/> -- The operation is carried as the text of the
  <delta> element as a Base64 encoded protocol buffer of the
  message type protocol::ProtocolSignedDelta.

applied-delta
-------------

The <applied-delta/> element contains a delta which has been
successfully applied to a wavelet by a wave server, along with
supplementary information about the result of the application.

* <operation/> -- The operation is carried as the text of the
  <applied-delta> element as a Base64 encoded protocol buffer
  of the message type protocol::ProtocolAppliedWaveletDelta.

Update Stanzas
~~~~~~~~~~~~~~

The wavelet-update operation is sent as a Message stanza.

wavelet-update
--------------

The <wavelet-update/> element is used within a Message stanza.  It is
used to push new wavelet operations applied to a local wavelet to the
wave providers of any remote participants.

When the requester is resending updates after reconnecting a XMPP
stream, the <wavelet-update/> MAY omit the <applied-delta/> elements
but MUST resend the <commit-notice/> elements.  In this case the
<commit-notice/> informs the receiver of the existence of updates,
and it is up to the receiver to request these using a <history-
request/> on a service stream.

Successful update and response example
--------------------------------------

An example of otherwave.com's federation host pushing data to a
federation remote:

Step 1: otherwave.com's federation host sends an update to the
federation remote::

   <message type="normal"
     from="wave.initech-corp.com"
     id="1-1" to="wave.acmewave.com">
     <request xmlns="urn:xmpp:receipts"/>
     <event xmlns="http://jabber.org/protocol/pubsub#event">
       <items>
         <item>
           <wavelet-update
             xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
             wavelet-name="wave://acmewave.com/initech-corp.com$w+a/conv+b">
             <applied-delta><![CDATA[CiI...MwE] ]></applied-delta>
           </wavelet-update>
         </item>
       </items>
     </event>
   </message>


Step 2: The federation remote acknowledges the update, indicating
success.::

   <message id="1-1"
     from="wave.acmewave.com"
     to="wave.initech-corp.com">
     <received
       xmlns="urn:xmpp:receipts"/>
   </message>

Service Stanzas
~~~~~~~~~~~~~~~

The service stanzas are for the submission of operations and wavelet
history retrieval.

History Request
---------------

The <delta-history/> element is used within a PubSub [XEP0060]_ event.
It is sent by a federation remote to request wavelet operations from
the hosting wave providers.  The response by the host provider's
federation host will contain the operations for the requested version
range.

*  'wavelet-name' -- REQUIRED attribute.
*  'start-version' -- REQUIRED attribute with version number
   (inclusive) from which to retrieve the wavelet's history.  (Note
   that the start version MUST fall on a delta boundary).
*  'start-version-hash' -- REQUIRED attribute with the hash for the
   associated start version.
*  'end-version' -- REQUIRED attribute with ending version number
   (exclusive) up to which to retrieve the wavelet's history.  (Note
   that the end version MUST fall on a delta boundary).
*  'end-version-hash' -- REQUIRED attribute with the hash for the
   associated end version.
*  'response-length-limit' -- OPTIONAL attribute containing advice
   from the requester about the preferred response limit, measured as
   the aggregate number of characters in the XML serialization of the
   applied deltas in the response.  The responder is advised but not
   required to respect the limit.  Moreover, the responder may
   operate with a lower limit of its own and send back a smaller
   message than requested.  When the responder exercises either its
   own or the requester's limit, it will return only a prefix of the
   requested wavelet deltas.  Unless the version range is empty, the
   responder will always return a minimum of one wavelet delta (the
   first) even if its length exceeds the responders or requester's
   limits.

History Response
----------------

The response to a History Request contains:

* <history-truncated> -- OPTIONAL attribute indicating that the
  returned deltas were truncated at the given version number
  (exclusive).  Truncation will occur if the <delta-history/>
  (Section 3.3.4.1) specified a 'response-length-limit' attribute or
  the responder imposed its own limit.  
* <applied-delta/> -- the update contains ZERO OR MORE <applied-
  delta/> elements, starting from the requested version up to the
  requested end version (exclusive), or until the latest version if
  the request did not contain the end version, or up to the version
  indicated in 'version-truncated-at'.
* <commit-notice/> -- OPTIONAL element indicating that some range of
  the returned deltas has not been committed to persistent storage
  by the hosting wave server.  The <commit-notice/> indicates up to
  which version the server has committed.

Successful history request / history response example
-----------------------------------------------------

Step 1: A federation remote makes a history-request (Section 3.3.4.1)
to the acmewave.com federation host::

  <iq type="get" id="1-1" from="wave.initech-corp.com" to="wave.acmewave.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <items node="wavelet">
        <delta-history
          xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
          start-version="12"
          start-version-hash="..."
          end-version="2345"
          end-version-hash="..."
          response-length-limit="300000"
          wavelet-name="wave://acmewave.com/initech-corp.com$w+a/conv+b"/>
      </items>
    </pubsub>
  </iq>

Step 2: acmewave.com's federation host returns the requested history.::

  <iq type="result" id="1-1" from="wave.acmewave.com" to="wave.initech-corp.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <items>
        <item>
          <applied-delta
            xmlns="http://waveprotocol.org/protocol/0.2/waveserver">
              <![CDATA[CiI...MwE] ]>
          </applied-delta>
        </item>
        <item>
          <commit-notice
            xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
            version="2344"/>
        </item>
        <item>
          <history-truncated
            xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
            version="2300"/>
        </item>
      </items>
    </pubsub>
  </iq>

Submit Request
--------------

The <submit-request/> element is used within a PubSub [XEP0060]_
event.  The federation remote submits wavelet operations to the
hosting wave provider.  A <submit-response/> will be returned.

*   <delta/> -- REQUIRED delta element to be submitted.

submit-response
---------------

A <submit-response/> element is used within a PubSub [XEP0060]_
response.  It is returned by a federation host after the hosting wave
server has processed the submitted delta.

* 'operations-applied' -- REQUIRED attribute with the number of
  operations applied by the wave server after transforming the
  submitted delta. The value of this attribute is a 32-bit integer
  to base 10.
* 'application-timestamp' -- REQUIRED timestamp (milliseconds since
  1.1.1979) attribute recording the time of delta application.
  The value of this attribute is a 64-bit integer to base 10.
* 'error-message' -- OPTIONAL string attribute containing an error
  message if the an error occurred while applying the delta.  Note
  it's possible to partially apply a delta, in which case the error
  message will be present.
* <hashed-version/> -- REQUIRED element with the version and history
  hash of the wavelet after the submitted delta was applied.

Successful submit request / submit response example
---------------------------------------------------

Step 1: The federation remote makes a submit request to the initech-
corp.com federation host::


  <iq type="set" id="1-1" from="wave.initech-corp.com" to="wave.acmewave.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <publish node="wavelet">
        <item>
          <submit-request
            xmlns="http://waveprotocol.org/protocol/0.2/waveserver">
            <delta wavelet-name="wave://acmewave.com/initech-corp.com$w+a/conv+b">
              <![CDATA[CiA...NvbQ==] ]>
            </delta>
          </submit-request>
        </item>
      </publish>
    </pubsub>
  </iq>

Step 2: The initech-corp.com federation host returns a response to
the submit request.::

  <iq type="result" id="1-1" from="wave.acmewave.com" to="wave.initech-corp.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <publish>
        <item>
          <submit-response
            xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
            application-timestamp="1234567890"
            operations-applied="2">
            <hashed-version
              history-hash="..."
              version="1234"/>
          </submit-response>
        </item>
      </publish>
    </pubsub>
  </iq>

Wavelet Update
##############

Wavelet update operations mutate wavelets.  The actual operation is a
signed protocol buffer that is included in the applied-delta element
Base64 encoded text.  The wavelet update MAY contain zero or more applied-
delta's and an optional commit-notice.  The wavelet update response
is an XMPP receipt of the form specified in XEP-0184.

Here is an example exchange::

  <message type="normal" from="wave.initech-corp.com" id="1-1" to="wave.acmewave.com">
    <request xmlns="urn:xmpp:receipts"/>
    <event xmlns="http://jabber.org/protocol/pubsub#event">
      <items>
        <item>
          <wavelet-update xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
	    wavelet-name="wave://acmewave.com/initech-corp.com$w+a/conv+b">
            <applied-delta><![CDATA[CiIKIAoFCNIJEgASF2ZvenppZUBpbml0ZWNoLWNvcnAuY29tEgUI0gkSABgCINKF2MwE] ]></applied-delta>
          </wavelet-update>
        </item>
      </items>
    </event>
  </message>


  <message id="1-1" from="wave.acmewave.com" to="wave.initech-corp.com">
    <received xmlns="urn:xmpp:receipts"/>
  </message>


Get Signer
##########

A remote wave server issues a signer-request to request certificates
for wavelets where the signer of the wavelet is currently unknown.
i.e. the request is being sent in response to a received wavelet update.
A signer is identified by a sequence of bytes as used in
protocol::ProtocolSignature.signer_id. The request is sent to the wave
server that hosts the wavelet. The request MUST NOT be sent to a remote
wave server. The provided history-hash identifies the delta for which
the certificate is being requested.

Here is an example exchange::

  <iq type="get" id="1-1" from="wave.initech-corp.com" to="wave.acmewave.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <items node="signer">
        <signer-request xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
          history-hash="somehash" version="1234"
          wavelet-name="wave://acmewave.com/initech-corp.com$w+a/conv+b"/>
      </items>
    </pubsub>
  </iq>

The hosting wave server replies with a chain of certificates sent
Base64 encoded in the certificate elements.  Each certificate element
represents a single certificate.  The order of the certificate
elements goes from the first which is the closest certificate, to the
last certificate which is the root for the certificate chain.  More
details on signing are still to be added to this document.::

  <iq type="result" id="1-1" from="wave.acmewave.com" to="wave.initech-corp.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <items>
        <signature xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
          domain="initech-corp.com" algorithm="SHA256">
          <certificate><![CDATA[Q0VS...VElPTg==] ]></certificate>
          <certificate><![CDATA[QkV...LRQ==] ]></certificate>
        </signature>
      </items>
    </pubsub>
  </iq>

The certificates are base64 encoded (almost as in PEM).
A (shortened) PEM certificate as follows::

  -----BEGIN CERTIFICATE-----
  MIIDmTCCAwKgAwIBAgIJAJbxsoszxI+5MA0GCSqGSIb3DQEBBQUAMIGQMQswCQYD
  qBgkuse74JojLyzxHg==
  -----END CERTIFICATE-----

is compressed by omitting the first and last line and by removing
the line break, resulting in::

  MIIDmTCCAwKgAwIBAgIJAJbxsoszxI+5MA0GCSqGSIb3DQEBBQUAMIGQMQswCQYDqBgkuse74JojLyzxHg==

This value is base64 encoded and sent as text or CDATA inside the
certificate element. Upon receiving the response, the remote wave
server must compute the signer ID from the list of certificates
to check that the received certificates match the requested signer.
Therefore, the remote wave server decodes the certificates, encodes
them using DER (ASN.1 Distinguished Encoding Rules), reversers their
order, i.e. the root certificate of the chain is first and the
closest certificate is the last. These DER encoded certificates are
treated as an ASN.1 SEQUENCE OF data type and are concatenated
accordingly resulting in a byte sequence. Finally, a SHA256 hash
of the resulting byte sequence is computed. The hash must match the
signer ID used in the signed delta.

A ASN.1 SEQUENCE OF is enocded as follows::
  
  Byte 1: 0b110000 (this indicates the data type, i.e. SEQUENCE OF)
  Byte 2: 0b10000000 OR (the number of bytes required to express the length of the data as a binary number)
  Bytes 3 .. n: The data length encoded as a big endian number
  Bytes n+1 ff.: The data

For example, a data of length 0x293 is encoded as::
  
  Byte 1: 0b110000
  Byte 2: 0b10000010
  Byte 3: 0x02
  Byte 4: 0x93
  Bytes 5 ff.: The data
  
Post Signer
###########

Before its first submit request to a hosting wave server, a remote wave
server MUST supply the certificate chain that will allow the hosting
wave server to authenticate the signed wave delta.
If the signature is not sent in advance, the hosting wave server will
reject the submit request and the remote wave server must send the signature
before it resends the submit request. More details on signing are still
to be added to this document.

Here is an example exchange::

  <iq type="set" id="1-1" from="wave.initech-corp.com" to="wave.acmewave.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <publish node="signer">
        <item>
          <signature xmlns="http://waveprotocol.org/protocol/0.2/waveserver"
            domain="initech-corp.com" algorithm="SHA256">
            <certificate><![CDATA[Q0V...Tg==] ]></certificate>
            <certificate><![CDATA[QkV...RQ==] ]></certificate>
          </signature>
        </item>
      </publish>
    </pubsub>
  </iq>


The hosting wave server acks the message.::

  <iq type="set" id="1-1" from="wave.initech-corp.com" to="wave.acmewave.com">
    <pubsub xmlns="http://jabber.org/protocol/pubsub">
      <publish>
        <item node="signer">
          <signature-response xmlns="http://waveprotocol.org/protocol/0.2/waveserver" />
        </item>
      </publish>
    </pubsub>
  </iq>

The encoding of the certificates are the same as in the signer request.

Documents
#########

A document is a sequence of items, where each item is a character, a
start tag, or an end tag.  Each item has a key-value map of
annotations.

Characters are Unicode code points.  Certain control characters,
special characters and noncharacters are not permitted.

Start tags consist of a type and attributes.  The type is an XML
name.  The attributes form a key-value map, where keys and values are
strings.  Certain Unicode control characters, special characters and
noncharacters are permitted neither in the type nor in attribute
names or values.

Each end tag terminates the rightmost unterminated start tag; the tag
name is implicit.  The number of start tags in the document equals
the number of end tags, and for every prefix of the document, the
number of start tags equals or exceeds the number of end tags.  Thus,
start and end tags nest properly, and there are no self-closing tags.

Annotation keys and values are strings.  Certain Unicode control
characters, special characters and noncharacters are not permitted.
If the map has no entry for a given key, we sometimes say that the
value for that key is null.  While each item conceptually has its own
annotation map, implementations may find it more efficient to have
just one annotation map for each consecutive run of items with the
same annotations.

Note that a naive serialization of the document without annotations
into a string is not formally an XML document because it can have
multiple elements and characters at the top level, while XML requires
a single root element.  How to interpret the document as XML is up to
the application; options include making sure at the application level
that the entire document contents are inside a single element even if
the protocol does not enforce this; ignoring all content other than
the first element; or wrapping the entire document in an implicit
root element whose type and attributes are not represented inside the
document.

Document operations
===================

A document operation is a set of instructions that specify how to
process an input document, reading its sequence of items from left to
right, to generate an output document.  For the purpose of this
specification, the operation does not modify the input document,
although implementations that perform destructive updates are
possible.

Document operations are invertible; for any document operation op
that turns an input document A into an output document B, an inverse
operation that turns B into A can always be derived from op without
knowledge of A or B.

A document operation consists of a sequence of document operation
components that are executed in order.  During this process, two
pieces of state need to be maintained in addition to the document
being processed:

* the current location ('cursor') in the input document, either to
  the left of the first item, between two items, or to the right of
  the last item of the input document, and
* the current annotations update, which is a map of annotation keys
  to pairs (old-value, new-value), where old-value and new-value are
  either null or an annotation value.
    
Initially, the cursor is to the left of the first item, and the
annotations update is empty.

After the final component, the annotations update must be empty, and
the cursor must be to the right of the last item in the input
document.

Document operation components
=============================

Document operation components can be divided into four classes:

*  update components (retain, replaceAttributes, updateAttributes)
   move the cursor over a consecutive range of input items and
   generate corresponding but potentially modified items in the
   output document;
*  insertion components (characters, elementStart, elementEnd)
   generate items in the output document without moving the cursor;
*  deletion components (deleteCharacters, deleteElementStart,
   deleteElementEnd) move the cursor over a consecutive range of
   input items without generating any output;
*  annotation boundaries (annotationBoundary) change the current
   annotations update but do not directly affect the document or the
   cursor.

The different component classes have the following interaction with
annotations:

*  For update components, the old values in the annotations update
   match the annotation values of each item in the input document
   that the component processes.  The generated items in the output
   document will have the same annotations as the corresponding input
   items, except for the annotation keys in the annotations update;
   for those keys, the generated items will have the new values.
*  For insertion components, the old values in the annotations update
   match the annotations of the item to the left of the cursor.  The
   inserted items are annotated with the new values from the
   annotations update in addition to any annotations on the item to
   the left of the cursor with keys that are not part of the
   annotations update.
   If the cursor is at the beginning of the document, the old values
   in the annotations update are null, and the inserted items are
   annotated with the new values from the annotations update.
*  For deletion components, the old values in the annotations update
   match the annotations of each item in the input document processed
   by the component, and the new values match the annotations of the
   rightmost item generated so far.  All annotation keys that have
   different values in the processed item and the rightmost item
   generated so far are present in the annotations update.
   If no items have been generated so far, the new values are null,
   and all annotation keys of the deleted items must be present in
   the annotations update.

retain(itemCount)  
   The cursor moves over the next itemCount items,
   and they are copied to the output document, with annotations as
   described above.  The argument itemCount is a positive integer.
replaceAttributes(oldAttributes, newAttributes)  
   The cursor moves over the next item, which must be a start tag with the
   attributes oldAttributes.  A start tag with the same type but the attributes
   newAttributes is generated in the output.  Its annotations are as described
   above.  The arguments oldAttributes and newAttributes are key-value maps.
updateAttributes(attributesUpdate)  
   The cursor moves over the next item, which must be a start tag.  A start tag
   with the same type is generated in the output, with annotations as described
   above.  The argument attributesUpdate is a map of attribute names to pairs
   (oldValue, newValue), where oldValue and newValue are either null or an
   attribute value.  The oldValues match the attributes of the start tag in the
   input document; an oldValue of null means no such attribute is present.  The
   generated start tag has the new values for the attributes in
   attributesUpdate.  Attributes in the input whose names are not listed are
   transferred to the output unchanged.
characters(characters)  
   The specified characters are inserted into the output document, with
   annotations as described above.
elementStart(type, attributes)  
   An element start with type type and attributes attributes is inserted into
   the output document, with annotations as described above.  This component
   must be terminated with an elementEnd.  Between an elementStart and its
   corresponding elementEnd, only insertion components are permitted.
elementEnd  
   An element end is inserted into the output document, with annotations as
   described above.  This component terminates the most recent unterminated
   elementStart.  It must not occur without a corresponding elementStart.
deleteCharacters(characters)  
   This component moves the cursor over the specified characters in the input
   document without generating any output.  The characters must match the
   actual characters in the input document, and the current annotations update
   must match as described above.
deleteElementStart(type, attributes)  
   This component moves the cursor over the specified element start in the
   input document without generating any output.  There must be an element
   start to the right of the cursor, and its type and attributes must match the
   arguments.  The current annotations update must match as described above.
   This component must be terminated with a deleteElementEnd.  Between a
   deleteElementStart and its corresponding deleteElementEnd, only deletion
   components are permitted.
deleteElementEnd  
   This component moves the cursor over an element end in the input document
   without generating any output.  There must be an element end to the right of
   the cursor.  The current annotations update must match as described above.
   This component terminates the most recent unterminated deleteElementStart.
   It must not occur without a corresponding deleteElementStart.
annotation-boundary(ends, changes)  
   This component modifies the current annotations update.  Ends is a set of
   annotation keys; these keys are removed from the annotations update.
   Changes is a map of annotation keys to pairs (oldValue, newValue), where
   oldValue and newValue are either null or an annotation value; these entries
   are added to the annotations update, or replace entries in the annotations
   update that have the same key.  The keys in ends and changes must be
   disjoint.  An operation must not contain two consecutive annotationBoundary
   components.  Ends must only contain keys that are part of the current
   annotations update.


Copyright and Patent
####################

This specification is made available under the terms of the `Creative Commons
Attribution 3.0 License <http://creativecommons.org/licenses/by/3.0>`_ and the
`Patent License <http://www.waveprotocol.org/patent-license>`_.

.. [RFC2119] Bradner, S., "`Key words for use in RFCs to Indicate Requirement Levels <http://bitworking.org>`_", BCP 14, RFC 2119, March 1997.
.. [RFC3920] XMPP Core
.. [XEP0060] XMPP Publish Subscribe 
.. [OT] XMPP Publish Subscribe 
.. [VERFED] Verfiable Federation

Protocol Schema
###############

The protocol schema, as Relax NG Compact:

.. include:: waveschema.rnc 
     :literal: 

Protocol Buffers
################

The protocol buffer definitions:

.. include:: ../../src/org/waveprotocol/wave/federation/federation.protodevel
     :literal: 
 
