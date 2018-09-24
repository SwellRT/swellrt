
# Federation

The original XMPP implementation of the Wave Federation protocol was removed from Wave's code base. SwellRT provides a new implementation based on the modern protocol [Matrix](http://matrix.org/). This work was performed by @waqee as part of its grant [Google Summer of Code 2016](https://github.com/Waqee/incubator-wave/commits/master).
This implementation hasn't been thorougly tested yet. Please use it carefully.

For convenience, we provide first a set up guide. Afterwards, deeper information about Wave+Matrix federation is introduced.

## Set up Guide

Examples on this guide are for Ubuntu Linux.

### Set up a Matrix Server

In this guide we use reference implementation of Matrix server, *synapse*.

First install dependencies:

```
sudo apt-get install build-essential python2.7-dev libffi-dev \
                     python-pip python-setuptools sqlite3 \
                     libssl-dev python-virtualenv libjpeg-dev libxslt1-dev
```

Get and install a Matrix server. 

```
virtualenv -p python2.7 ~/.synapse
source ~/.synapse/bin/activate
pip install --upgrade setuptools
pip install https://github.com/matrix-org/synapse/tarball/master
```

Create configuration for a server's instance named *example*. Set the right server name for
your case:

```
cd ~/.synapse

mkdir example
cd example

python -m synapse.app.homeserver \
    --server-name example.localhost \
    --config-path homeserver.yaml \
    --generate-config \
    --report-stats=no
```

Edit config file "homeserver.yaml" and enable registration (line 312).
Also replace content from lines 144-167 with:

```
# Number of messages a client can send per second
rc_messages_per_second: 10

# Number of message a client can send before being throttled
rc_message_burst_count: 100.0

# The federation window size in milliseconds
federation_rc_window_size: 1000

# The number of federation requests from a single server in a window
# before the server will delay processing the request.
federation_rc_sleep_limit: 100

# The duration in milliseconds to delay processing events from
# remote servers by if they go over the sleep limit.
federation_rc_sleep_delay: 500

# The maximum number of concurrent federation requests allowed
# from a single server
federation_rc_reject_limit: 50

# The number of federation requests to concurrently process from a
# single server
federation_rc_concurrent: 30
```

Remember to edit host and port properties. For more details, check out comments in the "homeserver.yaml" file.

Start the server. From the instance folder ("~/synapse/example") execute the command "synctl start" in the "~/synapse/bin" folder:

```
cd ~/synapse/example
../bin/synctl start
```

For the first time, create a Matrix user for the Wave server. 

```
../bin/register_new_matrix_user -c homeserver.yaml https://localhost:8448 -u wave -p 123123 -a
```

### Configure Wave Server

A predefined configuration of the Wave server federation following the Matrix parameters in the example is at "wave/config/reference.conf".

Copy paste .crt and .key files from Matrix server  "~.synapse/example" folder to the Wave's server folder.

Just start the server. To test federation, create a collaborative object (aka wave), then add a participant from other domain. The second participant then could open the object and use it.

### Set up in single physical server

For testing purposes you can install and federate two wave servers in the same box. 

In this scenario we will run two servers in domains *example.localhost* and *test.localhost*.

Generate the distribution package of SwellRT from source code:

```
./gradlew createDistBinTar

or

./gradlew createDistBinZip
```

Extract the generated file and create two copies *swellrt-example/* and *swellrt-test/*:

```
cd distributions/
tar zxvf swellrt-bin-X.Y.Z-beta.tar.gz
mv swellrt/ swellrt-example
cp -R swellrt-example/ swellrt-test
```

we provide two configuration files for this scenario. From the source code folder, copy *wave/config/application.conf.fed-example* and  *wave/config/application.conf.fed-test* to the standalone installations:

```
cp wave/config/application.conf.fed-example distributions/swellrt-example/config/application.conf

cp wave/config/application.conf.fed-test distributions/swellrt-test/config/application.conf
```

Create a two instances of Synapse following previous instructions for the two different domains (example.localhost and test.localhost) and for the *test.localhost* server use port 8449

Remind to copy certificates files to the root folder of each SwellRT installations:

```
cp ~/.synapse/example/*.crt distributions/swellrt-example/
cp ~/.synapse/example/*.key distributions/swellrt-example/

cp ~/.synapse/test/*.crt distributions/swellrt-test/
cp ~/.synapse/test/*.key distributions/swellrt-test/
```

In this single box environment, Matrix servers will need to resolve each other's host/port using a DNS record. (As long as both servers are in the same host, DNS is the only way to resolve the right port).

By installing a Bind9 DNS server you can use the following configuration for example.

Content of "/etc/bind/db.local"
```
$TTL    604800
@       IN      SOA     localhost. local.net. (
                              4         ; Serial
                         604800         ; Refresh
                          86400         ; Retry
                        2419200         ; Expire
                         604800 )       ; Negative Cache TTL
;
@       IN      NS      localhost.
@       IN      A       127.0.0.1
@       IN      AAAA    ::1
example IN      A       127.0.0.1
test    IN      A       127.0.0.1
;
_matrix._tcp.example.localhost. 3600    IN      SRV     10 0 8448 localhost.
_matrix._tcp.test.localhost.    3600    IN      SRV     10 0 8449 localhost.
```

Remind you might need to configure the box's network configuration to use "127.0.0.1" as DNS server. To check if DNS works properly use the following query:

```
nslookup -querytype=srv _matrix._tcp.example.localhost
```

*note*: in some servers, you will have to deactivate local DNS cache, for example in Ubuntu, edit */etc/NetworkManager/NetworkManager.conf* and comment "dns" entry.

Start both synapse servers and both SwellRT servers (use scripts `run-server.sh` or `run-server.bat`)
For each server, create an user:

Go to http://example.localhost:9898/create-user.html and use following data:

```
username: example
password: example
```

Go to http://test.localhost:9899/create-user.html and use following data:

```
username: test
password: test
```

In the *example* server create the pad (aka collaborative object) with
id *example.localhost/fedpad* using the user *example@example.localhost* user


```
http://example.localhost:9898/test-pad.html?id=example.localhost%2Ffedpad&user=example@example.localhost&pass=example
```

Add as participant the user from the other server *test@test.localhost*. 
Then, open this pad from the *test* server:

http://test.localhost:9899/test-pad.html?id=example.localhost%2Ffedpad&user=test@test.localhost&pass=test

Now, both users must see and edit the same text.



## Wave Federation Protocol

The Wave Federation Protocol allows Wave servers (with different domains) to share wavelet updates each other. This enables that users from a Wave server to participate in waves from other server. The main characteristics of the protocol are:


- The protocol manages sharing of wavelets.
- The wavelet id determines which domain owns it: **acme.com/master+data** 
- Wavelet Name = waveletId + waveId serialization: **acme.com/master+data/hanna.com/s+3x5ht67**
- A participantId’s domain determines where to a server sends wavelet updates.


Example: client submit a delta to the server

- Server’s wave state: version 15.
- Client submits delta at version 10: D 10
- Server accepts, transforms D 10 to D’ 15
- Applies D’ 15 operations to current state.
- Sends result (V 16 , HH 16 ) to the client.
- Signs the Delta with its private key (stored in
PKCS#8).
- Server sends AppliedDelta to all Fed Remotes with
participants on the wave. (Optionally also its “signer
info”, CA signed X.509 cert).


A Wave is just a global unique identifier associated with a domain. 
A Wavelet is data identified by a globally unique wavelet id, which is a domain name and an id string. The domain identifies the wave provider where the wave was originated (its owner). Wavelets belongs to a Wave.


Wavelets are hosted by the Wave provider of the creator. Wavelets in the same wave can be hosted by different Wave providers. However, user data is not federated; i.e., not shared with other wave providers.


![Wave, Wavelets and Blips](../resources/wave-wavelet-blip.png)

A Wave provider operates a Wave service on one or more networked servers. The central pieces of the Wave service is the Wave store, which stores wavelet operations, and the Wave server, which resolves wavelet operations by operational transformation and writes and reads wavelet operations to and from the Wave store. 
Typically, the Wave service serves waves to users of the Wave provider which connect to the Wave service frontend.

For the purpose of federation, the Wave service shares wavelets with participants from other providers by communicating with these Wave provider's servers. Copies of wavelets are distributed to all Wave providers that have participants in a given wavelet. Copies of a wavelet at a particular provider can either be local or remote. We use the term to refer to these two types of wavelet copies (in both cases, we are referring to the wavelet copy, and not the wavelet). A wave view can contain both local and remote wavelet copies simultaneously.


The originating Wave server is responsible for the hosting and the processing of wavelet operations submitted by local participants and by remote participants from other Wave providers. The Wave server performs concurrency control by ordering the submitted wavelet operations relative to each other using operational transformations. It also validates the operations before applying them to a local wavelet.

Remote wavelets are hosted by other providers, cached and updated with wavelet operations that the local provider gets from the remote host. When a local participant submits a wavelet operation to a remote wavelet, the Wave server forwards the operation to the Wave server of the hosting provider. Then the transformed and applied operation is echoed back and applied to the cached copy. Wave services use federation gateways and a federation proxy components to communicate and share waves with other wave providers. Federation gateways communicate local wavelet operations, push new local wavelet operations to the remote wave providers of any other participants, fulfill requests for old wavelet operations, and process wavelet operations submission requests. A Federation proxy communicates remote wavelet operations and is the component of a wave provider that communicates with the federation gateway of remote providers. It receives new wavelet operations pushed to it from other providers, requests old wavelet operations, and submits wavelet operations to other providers.

## XMPP and Matrix

In the scenario presented above, we need an underlying protocol which sends and receives messages. Previously, [XMPP](https://xmpp.org/) (Extensible Messaging and Presence Protocol), served this purpose. This project involved replacing the XMPP protocol with a relatively new protocol for real-time communication named Matrix protocol. It offers several advantages over minimal functionality of XMPP:-

Matrix is lighter, easier to integrate with more client libraries, based on HTTP therefore more compatible with network elements. The history sync feature helps us in syncing communication.

## Matrix

Matrix is an open protocol for real-time communication. It is designed to allow users with accounts at one communications service provider to communicate with users of a different service provider via online chat, Voice over IP, and Video Telephony. That is, it aims to make real-time communication work seamsly between different service providers.

The Matrix standard specifies RESTful HTTP APIs for securely transmitting and replicating JSON data between Matrix-capable clients, servers and services. Clients send data by PUTing it to a ‘room’ on their server, which then replicates the data over all the Matrix servers participating in this ‘room’. This data is signed using a git-style signature to mitigate tampering, and the federated traffic is encrypted with HTTPS and signed with each server’s private key to avoid spoofing. Replication follows eventual consistency semantics, allowing servers to function even if offline or after data-loss by re-synchronizing missing history from other participating servers.

## Matrix as Wave Federation Transport

For using Matrix protocol, each Wave server also has a Matrix “homeserver” running alongside it with the same domain. Wave server communicated with the Matrix homeserver using a client named wave:[domain]. To send messages, we first need to make a room for communicating with the other server e.g user1@example.com needs to send message to user2@test.com, a room is created by Wave server example.com using its client wave:example.com and a hypothetical Matrix client of the other server i.e. wave:test.com is invited to the room. If the client or wave:test.com joins the room, that means the hypothetical server exists and federation data is communicated using structured JSON messages in the room. Pings are sent in each room at intervals to indicate that the other client is still alive. Each room only serves as a one way pipeline. For reverse communication another room would be created, i.e wave:test.com would create a room and invite wave:example.com.

![Federation](../resources/federation.png)


## Source Code Guide

The main code for this project was written in the source code directory “wave/src/main/java/org/waveprotocol/wave/federation/matrix/”. 

- **MatrixFederationTransport**

An implementation of FederationTransport for Matrix federation. Basically starts the Matrix federation listener in a separate thread.

- **MatrixFederationModule**

Module for setting up federation subsystem using guice dependency injection.

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
