#############################################
Google Wave Client-Server Protocol Whitepaper
#############################################

.. Use headers in this order #=~-_

:Authors: 
    Joe Gregorio

:Version: 2.0 - May 2010

This whitepaper is part of a series. All of the whitepapers
can be found on `Google Wave Federation Protocol site`_.

.. _Google Wave Federation Protocol site: http://www.waveprotocol.org/whitepapers


Editorial Notes
###############
To provide feedback on this draft join the wave-protocol 
mailing list at
`http://groups.google.com/group/wave-protocol <http://groups.google.com/group/wave-protocol>`_

This current draft only covers a small subset of the functionality
that is required to build a full client. Future drafts
will expand to cover more functionality. 

Introduction
############
This document describes the protocol by which a
wave client communicates with a wave server in order to 
create, read, and modify waves. The protocol is defined in
terms of JSON messages exchanged over WebSockets. 

Background
##########
There is already a protocol being defined to handle the federation 
of Waves, however it was designed as a server-to-server protocol and
is not well suited for clients.
What is needed is a lighter weight protocol that only captures
the needs of a client-server communication channel. The WebSockets protocol 
was chosen because it provides the two-way communication
channel needed to efficiently handle wave messages, while being light weight
and targeted to browsers, which are considered a primary platform 
for client developers. 

Scope
#####
This specification only covers the rudiments of the communication between
a client and a server. There are many things that are not covered by 
this specification at this time, such as authentication, authorization, 
how a client determines which server to talk to, or which port to use.   
This protocol is a very simple client/server protocol implementation, 
and does not reflect the Google Wave web client protocol
used in production today.

Data Model
##########
It is important to understand the `Wave Federation Protocol`_ 
and `Conversation Model`_ as a prerequisite to this specification. 

.. _Conversation Model: http://www.waveprotocol.org/draft-protocol-specs/wave-conversation-model
.. _Wave Federation Protocol: http://www.waveprotocol.org/draft-protocol-specs/draft-protocol-spec

Terminology
===========
The following terminology is used by this specification: 

* wave - a collection of wavelets 
* wavelet - a collection of named documents and participants, and the domain of operational transformation 
* document - a structured wave document
* wave message - a single message sent either from the client to the server or from the server to the client.  

Wave messages do not include the WebSocket opening handshake messages.

Operation
#########
This section assumes an elementary understanding of the theory of `Operational
Transforms`_. 

.. _Operational Transforms: http://www.waveprotocol.org/whitepapers/operational-transform

Protocol Version
================
In the current implementation the version of the protocol is carried in each
message and if the server does not understand the version sent it closes
the connection. Future revisions may have the client and server negotiate
for an agreed upon protocol version.

The version of the protocol used is 1.

Transport
=========
The protocol begins when a Wave client connects with a Wave server.
The connection is handled by the WebSockets protocol. After the connection
is initiated Wave messages are sent between the client and 
server encapsulated in WebSocket frames. Each message occupies
a single frame. 

Transport Error Conditions
==========================

WebSocket Errors
~~~~~~~~~~~~~~~~
TBD

Timeouts
~~~~~~~~
TBD

Error recovery
~~~~~~~~~~~~~~
TDB

Message Flow
============
There are two kinds of Wave requests, ProtocolOpenRequest
and ProtocolSubmitRequest. Communication begins when
a client sends a ProtocolOpenRequest to the server with the 
id of a Wave it wishes to monitor and/or mutate. After opening
a wave the client may send ProtocolSubmitRequests
to the server to manipulate the wave. The server will 
send ProtocolWaveletUpdates to the client as the server
representation of the wave changes.

Any error messages related to the opening of a wave
are sent back from the server in a ProtocolWaveletUpdate.

A client may send more than one ProtocolOpenRequest, one for
each wave that the client is interested in.

The client MUST send a ProtocolOpenRequest for each 
wave that the client is interested in. A client MUST NOT
send mutations for a wave id that it has not issued a
ProtocolOpenRequest for. The client must
wait for the server to acknowledge the ProtocolOpenRequest
before sending ProtocolSubmitRequests for the given
wave as it needs to include the document hash with
each ProtocolSubmitRequest. 

ProtocolOpenRequest
~~~~~~~~~~~~~~~~~~~
The ProtocolOpenRequest contains a wave id and 
a wavelet_id_prefix. Those two determine the set of 
wavelets that the client will be notified of changes
to. 

The wavelet_id_prefix may be shortened to match
a larger subset of wavelets, with the empty string
matching all wavelets in the given wave.  

The client can indicate if it supports snapshots when
it sends a ProtocolOpenRequest.

It also contains the protocol version number, which is
defined as 1, per the previous section on Protocol Version.


ProtocolWaveletUpdate
~~~~~~~~~~~~~~~~~~~~~
In response to a ProtocolOpenRequest the server may
send any number of ProtocolWaveletUpdate messages.
The ProtocolWaveletUpdate may contain a snapshot of 
the current wave state or it will contain one or more
ProtocolWaveletDelta messages that represent deltas
to be applied to wavelets that the client is monitoring.
The inclusion of the snapshot is determined by the 
server, it will only be sent on the first ProtocolWaveletUpdate,
and will only be sent if the client has indicated in its 
ProtocolOpenRequest that it supports receiving snapshots.

ProtocolWaveletUpdate messages will only be sent for 
wavelets that the client is an explicit participant in.

ProtocolSubmitRequest
~~~~~~~~~~~~~~~~~~~~~
This message contains a ProtocolWaveletDelta which the 
client requests the server to apply to a wave. Only one 
submit per wavelet may be outstanding at any one time.

The client specifies which version to apply the delta at, 
and the client is expected to transform deltas pending 
for submission against deltas received in 
ProtocolWaveletUpdates from the server.  

ProtocolWaveletDelta's are applied atomically and either 
fully succeed, or the whole delta will fail.

ProtocolSubmitResponse
~~~~~~~~~~~~~~~~~~~~~~
The ProtocolSubmitResponse acknowledges the ProtocolSubmitRequest
and if the delta was successfully applied it also supplies the
ProtocolHashedVersion of the wavelet after the delta, which 
the client will need to successfully submit future deltas
to the wavelet.

Closing a wave
~~~~~~~~~~~~~~
TBD

Specific Flows
##############

Search
======
TBD

Creating a new wave
===================
Creating a new wave is different from other flows
since neither the client nor the server have the wave
id. The client must generate a unique id for the wave
and send a ProtocolOpenRequest for that wave id. 

Entropy and Wave ID Length
~~~~~~~~~~~~~~~~~~~~~~~~~~
TBD

Serializing Protocol Buffers as JSON
####################################
There is no standard serialization of Protocol Buffers
into JSON. This section will define the serialization
that is used to construct Wave Messages from the protocol
buffers included in this specification.

Protocol buffer messages may be nested, so this serialization
algorithm must be applied recursively.

The root level message is emitted as a JSON object. Each
member of the message will be emitted as a key-value pair
in the JSON object. Each member's key name in
the JSON serialization is set to normalize(key), where 
normalize is a function that takes in the protocol
buffer member key name and returns a JSON utf-8 string.

normalize()
===========
TBD

Member value serialization
==========================
The serialization of a value for the key is dependent 
on the type and modifiers of that member. If the member
is flagged as 'repeated' then the serialized 
value will be a JSON array. The array will be filled
with the serialized values of the repeated members. 

Modifiers
=========
The following modifiers can be applied to message
values and they alter how the values are serialized.

repeated
~~~~~~~~
For each repeated member value, serialize it as
JSON according to the following rules and add the serialization
to the JSON array.

required
~~~~~~~~
Required parameters are always serialized into JSON.

optional
~~~~~~~~
Optional parameters are only serialized if they appear in the
protocol buffer.

string
======
A string member of a protocol buffer message is serialized 
as a JSON string.

int
===
An int32 or int64 member of a protocol buffer message 
is serialized as a JSON number.

bool
====
A bool value is serialized as a JSON number with a value of
1 for true and 0 for false.

enum
====
An enum value is serialized as a JSON string for the enumeration's value.

bytes
=====
A bytes value is hex encoded and serialized as a JSON string.

message
=======
A protocol buffer message is serialized by recursively applying
the rules in this section. 
 
Security
########

Securing the channel
====================
TBD

Authenticating the client
=========================
TBD

Authorization
=============
Authorization is covered in the `Access Control Whitepaper`_. 

.. _Access Control Whitepaper: http://www.waveprotocol.org/whitepapers/access-control

Client-Server Protocol Buffers
##############################
While the client server protocol is implemented as JSON over WebSockets, 
each Wave message is a JSON serialization of a protocol buffer. The 
protocol buffer definitions are defined as:

  TBD

Example Client-Server Flow
##########################

  TBD  

Appendix A - Open Source Implementation Notes
#############################################
The current open source implementation of the 
client-server protocol begins with the client
opening the wave "indexwave!indexwave". That
is currently an implementation detail and is not 
documented.  

