######################################
Access Control in Google Wave
######################################

:Authors:
  Jon Tirsen

:Version: 1.0 - May 2009

Google Wave's primary means of access control is the list of addresses that
participate on a wavelet and what access accounts has to these addresses. This
white paper outlines how the wave platform stores, exchanges and enforces
access control.

This whitepaper is part of a series. All of the whitepapers
can be found on `Google Wave Federation Protocol site`_.

.. _Google Wave Federation Protocol site: http://www.waveprotocol.org/whitepapers

Executive summary
#################

Wave access control is defined as:

* which individual or robot has access to a specific account,
* what access an account has to an address,
* and finally what access an address has to a wavelet (see `Google Wave Data
  Model and Client-Server Protocol`_ for more information on the wavelets).

.. _Google Wave Data Model and Client-Server Protocol: http://www.waveprotocol.org/whitepapers/internal-client-server-protocol

Access from individuals to accounts and accounts to addresses is defined and
enforced inside each wave provider and not specified in the standard.  Address
access is modeled as a graph where each edge in the graph grants access from
one address to another address. These edges are stored in waves and are
authorized by the wave provider that controls the domain of the addresses. The
access edges can be exchanged between wave providers through the normal wave
federation protocols.

Typically an account has access to a canonical address which is the entry point
for an account into this graph, although this is wave provider specific.
Operations are authorized at the source of each wave provider. If authorization
spans multiple wave providers the operation needs to be sent and verified along
the path of each of the involved wave providers.  Different levels of access to
a wavelet is still to be defined.

Accounts and addresses
======================

Account
  An account belongs to an end user or a robot. Exactly how accounts work is
  wave provider specific. For example, Google uses Google Accounts to store and
  authenticate accounts, so a Google Wave account is shared with other Google
  properties.

Address
  Most of the system does not deal directly with accounts but rather with
  addresses. An address is a string formatted as an email address (RFC 2822).
  Addresses, rather than accounts, participate in wavelets.

Canonical address
  Each account has a canonical address which is the address the user acts as
  normally. Most per-user metadata is stored with the canonical address as a
  key. The canonical address cannot generally be changed, but an account can
  participate on a single wavelet as multiple addresses.

Authentication
==============

Each wave provider chooses how they authenticate their users. In Google Wave we
use a simple username and password scheme for individuals. Robots are contacted
by the Google Wave provider through a well-defined URL and are therefore
authenticated that way.

Address access as a graph
=========================

Address access can be seen as a directed graph of address to address edges
where each edge is restricted by access settings.

.. image:: img/address-address.png

The entry point into the graph for a user or a robot is their canonical address.

.. image:: img/account-canonical.png

There are multiple types of access which indicate what address A can do as address B.

* Indexed to (INDEX) - wavelets addressed to address B will be written into the
  index of the account associated with address A (transitively).
* Add (ADD) - address A can add address B to wavelets.
* Add myself as (ADD_ME) - address A can add address A to wavelets as address B.
* Read (READ) - address A can read wavelets addressed to address B.
* Write (WRITE) - address A can do anything as address B. This could also be
  called "act as".
* Grant (GRANT) - address A can grant additional access
  edges to address B.

Storing and exchanging access edges
===================================

Wave representation of an access edge
  Access edges are stored in data documents in wavelets as follows:

::

  <grant from="a@example.com" to="b@example.com" until="2009-06-14T13:31Z">
    <access>INDEX</access>
    <access>READ</access>
  </grant>

Authorized access edge
  An authorized access edge is an access edge stored in a wave that can be
  attributed to an author that has a Grant access edge to the to address she is
  granting additional access too. This attribution should be enforced and
  verified by the wave provider. For example, the Google wave provider uses a
  namespace for all access edges. The namespace policy for that namespace will
  not allow edits that are not authorized.

Access wave
  Each account has an access wave identified by its canonical address. This
  contains the entire access sub-graph that is reachable from the canonical
  address. The wave provider of the account maintains this access wave by
  copying all the relevant and authorized access edges it encounters while
  indexing its own and its federated wave providers' waves. This means that
  access edges can be stored and distributed anywhere in the system as long as
  they are authorized as above. The storage and distribution mechanism of
  Google Wave itself is used to store and distribute this information.

Time to live
  When a wave provider issues an access edge to federated servers they are
  issued with a limited time period. They have to be refreshed within that time
  period or they are no longer valid. The time period should be chosen to
  minimize chattiness of the protocol and still allow for timely revocation.
  This is typically used for READ authorization when opening of a wavelet is
  not validated at the owning wave provider.

Authorizing an operation
========================

An operation always contains the path of authorization from the canonical
address to the address the account wants to perform an operation as excluding
any initial WRITE edges.  Using the information available in the access wave
the client builds the path and inserts it into the operation that it sends to
its wave provider. After it has optimistically applied the operation to the
wave in the client it sends it to its wave provider who then signs and forwards
the operation to the next wave provider in the path. Every wave provider on the
path will validate and sign the operation before it is finally forwarded and
applied at the wave provider that owns the wavelet. This final wave provider is
responsible for verifying all the signatures.

If an authorization fails, the client has typically already optimistically
applied the operation to the wave so will either need to reverse those
operations or indicate an error to the user. In a well-behaved system this
should only occur if an access edge has been removed or changed and this change
has yet to be forwarded to the clients wave provider. In this case the client
would access edges that are no longer valid.

Groups
######

Groups are implemented on top of this generic access framework. Each group has
an address and members. Group membership is expressed as the following edge for
each member of the group:

.. image:: img/member-group.png

As you can see an important detail of groups in wave is that being a member of
a group does not allow you to directly write into a wave which that group is
addressed to. Instead it lets you add yourself as a direct participant to that
wave.

A group can be a member of another group which looks like this:

.. image:: img/member-group-group.png

This means that wavelets addressed to both Group 1 and Group 2 will be written
into the member's index and the member can read and "write" (add self as a
participant) to all these wavelets.

Read-only groups mean that the "add myself" access is lacking:

.. image:: img/member-read-group.png

An address can be a read-write member of a nested group even though it's a
read-only member of an outer group:

.. image:: img/member-group-read-group.png

This means the member can become a participant of wavelets addressed to Group 1
but not to wavelets addressed to Group 2.

Delegation
==========

Delegation allows an account to perform operations with another address as the
author. Google Wave currently uses this for two cases:

* An account that is a write-member of a group can perform an AddParticipant
  operation to add an address belonging to that account to a wavelet.
* Google Wave's spelling ("Spelly"), linking ("Linky"), and other infrastructure
  services act on behalf of any address in a wavelet with those services
  enabled.

This last case is represented as the following edge:

.. image:: img/spelly.png

This provides Google Wave infrastructure services full access to act as a user,
while being authenticated as a service account.

Per-wavelet access control
==========================

Google Wave will eventually support some level of access control on a wavelets
but requirements and implementation plans have yet to be determined. For
example:

* A "commenter" role whereby a user can only create new blips and edit their own blips.
* A "confidential" mode (on the whole wavelet) or role (on a participant) where
  participants can't add new participants.

