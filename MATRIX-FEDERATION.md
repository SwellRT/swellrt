# Matrix Federation

The task of this google summer of code was to replace the underlying federation protocol of google wave/apache wave to use matrix protocol in instead of xmpp protocol.

## Wave Federation Protocol

Federation in apache wave is a server to server protocol using which messages are exchanged between different servers. Each client is registered with it's own server, however, if a client needs to talk to a client registered on a different server then federation is required. It is designed for near real-time communication between the computer supported cooperative work wave servers.

All waves and wavelets (child waves) are identified by a globally unique wave id, which is a domain name and an id string. The domain name identifies the wave provider where the wave originated. Waves and wavelets are hosted by the wave provider of the creator. Wavelets in the same wave can be hosted by different wave providers. However, user data is not federated; i.e., not shared with other wave providers. Private reply wavelets are also possible, of which other participants have no knowledge or access. If a private wavelet is sent between users on the same wave provider, it's not federated regardless of where the parent wave is hosted.

A wave provider operates a wave service on one or more networked servers. The central pieces of the wave service is the wave store, which stores wavelet operations, and the wave server, which resolves wavelet operations by operational transformation and writes and reads wavelet operations to and from the wave store. Typically, the wave service serves waves to users of the wave provider which connect to the wave service frontend. For the purpose of federation, the wave service shares waves with participants from other providers by communicating with these wave provider's servers. Copies of wavelets are distributed to all wave providers that have participants in a given wavelet. Copies of a wavelet at a particular provider can either be local or remote. We use the term to refer to these two types of wavelet copies (in both cases, we are referring to the wavelet copy, and not the wavelet). A wave view can contain both local and remote wavelet copies simultaneously.
The originating wave server is responsible for the hosting and the processing of wavelet operations submitted by local participants and by remote participants from other wave providers. The wave server performs concurrency control by ordering the submitted wavelet operations relative to each other using operational transformation. It also validates the operations before applying them to a local wavelet.
Remote wavelets are hosted by other providers, cached and updated with wavelet operations that the local provider gets from the remote host. When a local participant submits a wavelet operation to a remote wavelet, the wave server forwards the operation to the wave server of the hosting provider. Then the transformed and applied operation is echoed back and applied to the cached copy.
Wave services use federation gateways and a federation proxy components to communicate and share waves with other wave providers. Federation gateways communicate local wavelet operations, push new local wavelet operations to the remote wave providers of any other participants, fulfill requests for old wavelet operations, and process wavelet operations submission requests. A Federation proxy communicates remote wavelet operations and is the component of a wave provider that communicates with the federation gateway of remote providers. It receives new wavelet operations pushed to it from other providers, requests old wavelet operations, and submits wavelet operations to other providers.

## Role of Matrix

In the scenario presented above, we need an underlying protocol which sends are recieved messages. Previously, xmpp which stands for Extensible Messaging and Presence Protocol, served this purpose. My project involved replacing the xmpp protocol with a relatively new protocol for real-time communication named matrix protocol. It offers several advantages over minimal functionality of xmpp like better web support and history synchronization. However, in this case, it is essentially being used similarly as a message passing protocol.

## Matrix

Matrix is an open protocol for real-time communication. It is designed to allow users with accounts at one communications service provider to communicate with users of a different service provider via online chat, Voice over IP, and Videotelephony. That is, it aims to make real-time communication work seamlessly between different service providers.

The Matrix standard specifies RESTful HTTP APIs for securely transmitting and replicating JSON data between Matrix-capable clients, servers and services. Clients send data by PUTing it to a ‘room’ on their server, which then replicates the data over all the Matrix servers participating in this ‘room’. This data is signed using a git-style signature to mitigate tampering, and the federated traffic is encrypted with HTTPS and signed with each server’s private key to avoid spoofing. Replication follows eventual consistency semantics, allowing servers to function even if offline or after data-loss by re-synchronizing missing history from other participating servers.

## Matrix Implementation

For using matrix protocol, each wave server also has a matrix homeserver running along side it with the same domain. Wave server communicated with the matrix homeserver using a client named wave:[domain]. To send messages, we first need to make a room for communicating with the other server e.g user1@example.com needs to send message to user2@test.com, a room is created by wave server example.com using its client wave:example.com and a hypothetical matrix client of the other server i.e. wave:test.com is invited to the room. If the client or wave:test.com joins the room, that means the hypothetical server exists and federation data is communicated using structured json messages in the room. Pings are sent in each room at intervals to indicate that the other client is still alive. Each room only serves as a one way pipeline. For reverse communication another room would be created, i.e wave:test.com would create a room and invite wave:example.com.

## Source Code Guide

The main code for this project was written in the directory wave/src/main/java/org/waveprotocol/wave/federation/matrix/. Following is an overview of the interfaces and classes:-

- **MatrixFederationTransport**

An implementation of FederationTransport for Matrix federation. Basically starts the matrix federation listener in a separate thread.

- **MatrixFederationModule**

Module for setting up an XMPP federation subsystem using guice dependency injection.

- **OutgoingPacketTransport**

Generic outgoing Matrix packet transport interface. Should be implemented by the handling Matrix transport (e.g. application service system).

- **AppServicePacketTransport**

Implements OutgoingPacketTransport allowing users to send packets, and accepts an IncomingPacketHandler which can process incoming packets.

- **IncomingPacketHandler**

Generic incoming Matrix packet handler interface. This should only be implemented by {@link MatrixPacketHandler}, regardless of which wire transport is in use.

- **MatrixPacketHandler**

Provides abstraction between Federation-specific code and the backing Matrix transport, including support for reliable outgoing calls (i.e. calls that are guaranteed to time out) and sending error responses.

- **MatrixRoomManager**

Provides public methods to accept invite and join a room for receiving packets (via MatrixPacketHandler), as well as managing room creation/search for communication with remote Server via #getRoomForRemoteId.

- **RemoteRoom**

Represents Matrix room status for a specific remote domain. This class only exposes one public method; #getRoomForRemoteId.

- **MatrixFederationRemote**

Remote implementation. Receives submit and history requests from the local wave server and sends them to a remote wave server Host, and also receives update messages from a remote wave server Host and sends them to the local wave server.

- **MatrixFederationHost**

This class encapsulates the incoming packet processing portion of the Federation Host. Messages arrive on this class from a foreign Federation Remote for wavelets hosted by the local wave server.

- **MatrixFederationHostForDomain**

An instance of this class is created on demand for outgoing messages to another wave Federation Remote. The wave server asks the MatrixFederationHost to create these.

- **MatrixUtil**

Common utility code for Matrix JSON packet generation and parsing.

- **Base64Util**

Utility class for encoding and decoding ByteStrings, byte arrays and encoding generic protocol buffers.

- **Request**

Generic class for encapsulating an outgoing request.

- **SuccessFailCallback**

A generic onSuccess/onFailure callback interface.

- **PacketCallback**

Simple callback type used for receiving reliable Matrix JSON packets. This allows for clearly defined success and failure states.

## Set-up Guide Ubuntu

**Matrix Server Synapse installion**

**Open terminal to home directory. Run following commands**

sudo apt-get install build-essential python2.7-dev libffi-dev \
                     python-pip python-setuptools sqlite3 \
                     libssl-dev python-virtualenv libjpeg-dev libxslt1-dev

virtualenv -p python2.7 ~/.synapse
source ~/.synapse/bin/activate
pip install --upgrade setuptools
pip install https://github.com/matrix-org/synapse/tarball/master

cd ~/.synapse

mkdir example
cd example

python -m synapse.app.homeserver \
    --server-name example.localhost \
    --config-path homeserver.yaml \
    --generate-config \
    --report-stats=no

**Open homeserver.yaml**
**Go to line 312 and enable registration**
**Go to lines 144-167 and replace with following. just increasing rate limit**

\# Number of messages a client can send per second
rc_messages_per_second: 10

\# Number of message a client can send before being throttled
rc_message_burst_count: 100.0

\# The federation window size in milliseconds
federation_rc_window_size: 1000

\# The number of federation requests from a single server in a window
\# before the server will delay processing the request.
federation_rc_sleep_limit: 100

\# The duration in milliseconds to delay processing events from
\# remote servers by if they go over the sleep limit.
federation_rc_sleep_delay: 500

\# The maximum number of concurrent federation requests allowed
\# from a single server
federation_rc_reject_limit: 50

\# The number of federation requests to concurrently process from a
\# single server
federation_rc_concurrent: 30

**Run the following in previous terminal**

synctl start

cd ..
mkdir test
cd test

python -m synapse.app.homeserver \
    --server-name test.localhost \
    --config-path homeserver.yaml \
    --generate-config \
    --report-stats=no

**Open homeserver.yaml**
**Go to line 60 and change port to 8449**
**Go to line 93 and change port to 8009**
**Go to line 312 and enable registration**
**Go to lines 144-167 and replace with following. just increasing rate limit**

\# Number of messages a client can send per second
rc_messages_per_second: 10

\# Number of message a client can send before being throttled
rc_message_burst_count: 100.0

\# The federation window size in milliseconds
federation_rc_window_size: 1000

\# The number of federation requests from a single server in a window
\# before the server will delay processing the request.
federation_rc_sleep_limit: 100

\# The duration in milliseconds to delay processing events from
\# remote servers by if they go over the sleep limit.
federation_rc_sleep_delay: 500

\# The maximum number of concurrent federation requests allowed
\# from a single server
federation_rc_reject_limit: 50

\# The number of federation requests to concurrently process from a
\# single server
federation_rc_concurrent: 30

**Run the following in previous terminal**

synctl start

**Now the servers are created. We now need to register a wave client in both server. This is needed because sync requests for matrix app servers arent working yet so we need to use a normal client for our federation. Open https://localhost:8448/_matrix/client and https://localhost:8449/_matrix/client and register user with name "wave" and password "123123" on both.**

**Next we have to set up local srv records. First install bind9**

sudo apt-get install bind9

**Next go to /etc/bind/db.local and paste at the end**

_matrix._tcp.example.localhost.    IN      SRV     10 0 8448 localhost.
_matrix._tcp.test.localhost.   IN      SRV     10 0 8449 localhost.

**Add 127.0.0.1 to your network dns addresses and restart bind using following command**

sudo service bind9 restart

**Next clone the repository im working in twice rename the second one to incubator-wave2 something. Copy paste .crt and .key from ~.synapse/example folder to first clones wave directory and .crt a .key from ~.synapse/test to second clones wave directory.**

**Copy paste the clone-reference.conf file in root directory to the config/reference.conf of second clone.**

**Run both clones and hope it works :D**
