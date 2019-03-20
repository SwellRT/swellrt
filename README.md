# SwellRT [![Build Status](https://api.travis-ci.org/SwellRT/swellrt.svg?branch=master)](https://travis-ci.org/P2Pvalue/swellrt) [![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/P2Pvalue/swellrt)

SwellRT is an open source **backend-as-a-service**. It provides prebuilt features to speed up development of collaborative Web applications:

* Realtime storage (eventual consistency)
* Extensible text collaborative editor
* User management and authentication
* Server federation with [Matrix](http://matrix.org)
* Events and Bots (in development)



The main feature of SwellRT is realtime storage based in objects. They can be shared among participants that can mutate them in realtime.
All changes are persisted and propagated transparently. Object's state is eventually consistent.

Check out API basics:

```js

service.open({

	id: '<object-id>'

}).then(object => {

 	// Anyone can open the object
	object.setPublic(true);

});
```

Set and get a property:

```js

object.set('person', 
  { 
	name: 'Alice',
	city: 'New York'
  });
  
object.get('person.city');

  
```

Listen for updates (local or remote):

```js

object.node('person').addListener( event => {
	
	console.log('Property Updated ');

});	
```

Client libraries for Web and Java/Android (experimental) are provided.

## Documentation and examples

API documentation can be found [here](https://github.com/SwellRT/swellrt/blob/master/wave/doc/swellrt/Reference.md)

Basic examples can be found [here](https://github.com/SwellRT/swellrt/tree/master/wave/webapp). Try them running a SwellRT server
and visiting *http://localhost:9898*


## Running a SwellRT Server

You can build the server yourself or use our pre-built Docker image. 

### Building SwellRT from source code

Prerequisites

- [Java JDK 8](http://openjdk.java.net/install/)
- [MongoDB 3.2](https://docs.mongodb.com/manual/administration/install-community/)

Clone the project

```
git clone git@github.com:SwellRT/swellrt.git
cd swellrt
```

Build

```
./gradlew compileJava devWeb
```

If you get a "User limit of inotify watches reached" error, please increase this limit following the steps [here](https://askubuntu.com/questions/770374/user-limit-of-inotify-watches-reached-on-ubuntu-16-04).

Start the server

```
./gradlew run
```

Visit http://localhost:9898 to check server installation and try some demos.

### Standalone installation (Jar) 

To create a standalone installation of SwellRT, use the *createDistBinTar* or *createDistBinJar* tasks:

```
./gradlew createDistBinJar
```

The generated file is placed at *distributions/* folder.
Extracts the file and use the *run-server.sh* or *run-server.bat* scripts to start the server.


Edit configuration in *config/wave.conf* based on *config/reference.conf*.


### Docker

Get docker image of SwellRT (latest version by default). Check out all available SwellRT versions at [Docker Hub](https://hub.docker.com/r/p2pvalue/swellrt/):

```sh
$ docker pull p2pvalue/swellrt
```

Run docker container in deattached mode (-d). 

```sh
$ docker run \ 
-e MONGODB_HOST=<host> \
-e MONGODB_PORT=<port> \
-e MONGODB_DB=<db name> \
-p 9898:9898 \
-h swellrt \
--name swellrt \
-d p2pvalue/swellrt
```
This commands also binds default SwellRT server port 9898, to port 9898 in the host machine (-p 9898:9898). Sets hostname to "swellrt" (-h). And configures SwellRT to use a MongoDB server instance with the provided parameters.

*See following section to configure a MongoDB instance.*

**Persistent folders**

For productive installations of SwellRT, config and data folders should be outside the container. For example, to put all log files in host's folder **/var/log/swellrt**, run docker with *-v* parameter:

```sh
$ docker run -v /usr/local/swellrt/log:/var/log/swellrt  -p 9898:9898 -h swellrt -d p2pvalue/swellrt
```

These are all the folders you can map outside the container:

| Folder (Docker cointainer) | Description |
| -------------------------- | ----------- |
| /usr/local/swellrt/config | Server config files |
| /usr/local/swellrt/log | Server log files |
| /usr/local/swellrt/sessions | Persistent HTTP Sessions |
| /usr/local/swellrt/avatars | Users avatar images |
| /usr/local/swellrt/attachments | User files |

**Server config**

Server configuration can be adjusted by editing files in the **config/** folder. Default settings
can be found in the [repo](https://github.com/SwellRT/swellrt/tree/master/wave/config).

If you map the **config/** folder in your host machine, you must copy those files to it.

**Post installation**

Visit "http://localhost:9898" and "http://localhost:9898/chat" to check server installation and try some demos.


### MongoDB

This section explains how to install and configure a MongoDB server with SwellRT.

Get latest MongoDB Docker image

```sh
$ docker pull mongo
```

Run mongo container

```sh
$ docker run -p 27017:27017 --name mongo -d mongo
```

Run SwellRT (it assumes Docker containers are using [default bridge network](https://docs.docker.com/engine/userguide/networking/#the-default-bridge-network))

```sh
$ docker run \
-e MONGODB_HOST=172.17.0.1 \
-e MONGODB_PORT=27017 \
-e MONGODB_DB=swellrt \
-p 9898:9898 \
-h swellrt \
--name swellrt \
-d p2pvalue/swellrt
```

The database **swellrt** is created automatically if it doesn't exist.

## Federation

SwellRT servers can be federated using [Matrix protocol](http://matrix.org).
A set up guide and technical documentation can be found [here](https://github.com/SwellRT/swellrt/blob/master/wave/doc/swellrt/Matrix-Federation.md). 

## Contact and Support

Visit our [Gitter Channel](https://gitter.im/P2Pvalue/swellrt) or email to swellrt@gmail.com.

## Java/Android client

An experimental Java library is in package *org.swellrt.beta.client.platform.java*
Check out chat demo app in *org.swellrt.beta.client.platform.java.ChatDemo". 

## Contributing

Read our [contributing guide](https://github.com/prastut/swellrt/blob/install-docs-fix/CONTRIBUTING.MD) to learn more about our development process, how to propose bugfixes and improvements, and how to build and test your changes to SwellRT.

## Acknowledgments

The SwellRT project is a fork of [Apache Wave](http://incubator.apache.org/wave/).
Initial work of SwellRT has been funded by the EU research project [P2Pvalue](http://p2pvalue.eu) and supported by [GRASIA research group](http://grasia.fdi.ucm.es/) of the *Universidad Complutense of Madrid*.


## Cryptographic Software Notice

This distribution includes cryptographic software.  The country in
which you currently reside may have restrictions on the import,
possession, use, and/or re-export to another country, of
encryption software.  BEFORE using any encryption software, please
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to
see if this is permitted.  See <http://www.wassenaar.org/> for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this Apache Software Foundation
distribution makes it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception (see the BIS
Export Administration Regulations, Section 740.13) for both object
code and source code.

The following provides more details on the included cryptographic
software:

  Wave requires the BouncyCastle Java cryptography APIs:
    http://www.bouncycastle.org/java.html
