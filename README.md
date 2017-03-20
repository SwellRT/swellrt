# SwellRT [![Build Status](https://travis-ci.org/P2Pvalue/swellrt.svg?branch=master)](https://travis-ci.org/P2Pvalue/swellrt)

SwellRT is an open source **backend-as-a-service**. It allows to develop Web and _mobile_(*) apps faster by providing a set of common backend features:


**Features:**

* Real-time storage (NoSQL)
* User management
* Auth
* Event based integration

SwellRT enables easily real-time collaboration in your apps:

* Collaborative text editing
* Chats
* Reactive user interface
* Push notifications
* Ubiquitous User Experience across devices


**Federation:**

SwellRT servers can be federated using Matrix.org open protocol.


_(*) Note: native mobile clients are not still available_

### Build

 **Prerequisites**

- [Java JDK 8](http://openjdk.java.net/install/)
- [MongoDB 2.4](https://docs.mongodb.com/manual/administration/install-community/)

Clone the project

```
git clone git@github.com:P2Pvalue/swellrt.git
cd swellrt
```

Build

```
./gradlew compileJava devWeb
```

Start the server

```
./gradlew run
```

Visit "http://localhost:9898" to check server installation and try some demos.


### Docker

A docker image of the latest release of SwellRT is provided at [Docker Hub](https://hub.docker.com/r/p2pvalue/swellrt/):

```sh
$ docker pull p2pvalue/swellrt
```

This image doesn't include a MongoDB server. To pass connection data for a MongoDB instance to the container pass following environment variables to the container:

```sh
$ docker run -e MONGODB_HOST=<host> -e MONGODB_PORT=<port> -e MONGODB_DB=<db name> -p 9898:9898 -h swellrt -d p2pvalue/swellrt
```

Some SwellRT's configuration and data files must be stored out of the Docker container to avoid losing data when the image is updated. Map these folders in your host machine use following parameters:

```sh
$ docker run -v <host machine folder>:<docker image folder>  -p 9898:9898 -h swellrt -d p2pvalue/swellrt
```

List of docker's image folders that should be placed outside the container (in your host machine):

- `/usr/local/swellrt/config` Folder storing all config files. See documentation for details.
- `/usr/local/swellrt/log` Folder storing server log.
- `/usr/local/swellrt/sessions` Folder storing Web sessions.
- `/usr/local/swellrt/avatars` Folder storing user's avatar.
- `/usr/local/swellrt/attachments` Folder storing attachment files (when MongoDB is not used as attachment storage).

Visit "http://localhost:9898" to check server installation and try some demos.


### API (JavaScript Web)

At the moment, SwellRT only provides a JavaScript client for Web projects.

A good starting point is the source code of [form demo](https://github.com/P2Pvalue/swellrt/blob/master/wave/webapp/demo-form.html)and [text editor demo](https://github.com/P2Pvalue/swellrt/blob/master/wave/webapp/demo-pad.html)

#### Basic Concepts

The aim of SwellRT API is to provide easy programming of real-time collaboration. The programming model is based in two abstractions:


**Collaborators**

Users registered in any SwellRT server. Also there is a special _anonymous_ collaborator.
Each collaborator belongs to a server instance, and their ID has the syntax _(user name)@(server name)_


**Collaborative Objects**

A collaborative object is a data structure shared by one or more _collaborators_.
Each collaborator can have different level of access to an object and its parts.

An object, when it is used by a collaborator has three different data zones:

- Persisted zone: any change in this zone is automatically sync and persisted among collaborators.
- Private zone: only the current collaborator has access to this zone. This data is persisted.
- Transient zone*: any change in this zone is automatically among collaborators, but it is never persisted.

Each zone is a nested structured of arrays, maps and primitive values. For example, in Javascript it can be seen as
an object.

_(*) Transient zones are not still available._

#### First steps


First of all, make your Web project to load the client:

```
<script src='http://localhost:9898/swellrt-beta.js'></script>
```

Get a reference of the API instance (we name it, "service" or "s"):

```
<script>

  swellrt.onReady( (service) => {
    window.service = service;
   });

</script>    
```


**Login**

To handle objects a login is required:

```
    service.login({
      id : swellrt.Service.ANONYMOUS_USER_ID,
      password : ""
    })
    .then(profile => {  ...  });
```

Anonymous users are associated with the current browser session.

**Creating and getting objects**

_open()_ will load an object or create it, if it doesn't exist.
Leave _id_ field empty to create an object with an auto generated id.


```
	service.open({

	    id : "my-first-collaborative-object"

	})
	.then( (result) => {

		let obj = result.controller;
		obj.setPublic(true);

	})
	.catch( (exception) => {

	});
```

_obj_ is the view of the collaborative object for the logged in collaborator. It is the persisted zone.
For this example, lets set the object as _public_ in order to be accessible by other anonymous collaborators.

To get the personal zone use:

```
	let privObj = obj.getPrivateArea();
```


**Working with collaborative objects**


Using objects with map syntax

```
	// primitive values
	obj.put("name", "Kelly Slater");
	obj.put("job", "Pro Surfer");
	obj.put("age", 42);

	// add nested map
	obj.put("address", swellrt.Map.create().put("street", "North Coast Avenue").put("zip", 12345).put("city","Honololu"));

	// get root level keys (properties)
	obj.keys();

	// access values
	obj.get("address").get("street");

```



Using objects with JavaScript syntax
```
	// Get a javascript proxy
	jso = obj.asNative();

	// Reading properties
	jso.address.street;

	// Adding properties
	jso.address.state = "Hawaii";


	// Adding nested map - as mutable properties

	jso.quiver = swellrt.Map.create();
	jso.quiver.put("surfboard-1-size", "6.1, 18 1/2, 3 1/4");
	jso.quiver.put("surfboard-2-size", "5.11, 19 , 2 3/4");


	// Adding nested map - (bulk javascript)
	// the whole javascript object is stored as a single item,
	// changes in properties won't throw events.

	jso.prize = {
		contest: "Fiji Pro",
		year: "2015",
		points: 12000
	};


```

When an object is opened by different collaborators (e.g. in two different browser windows, open the same public object)
any change in a data property is automatically sync and updated in each open instance. If you want to listen when changes are made,
register a listener in a property:

```
	service.listen(jso.quiver, (event) => {

        // Note: this handler is invoked for local and remote changes.

        if (event.key == "surfboard-2-size" && event.type == swellrt.Event.UPDATED_VALUE) {
          let updatedValue = event.value.get();
        }

   });
```

## Contact and Support

Visit our [Gitter Channel](https://gitter.im/P2Pvalue/swellrt) or email to swellrt@gmail.com.

## Contributing

### Getting the source

Clone the SwellRT source code from the GitHub repo:

```sh
$ git clone https://github.com/P2Pvalue/swellrt
```

Please, send Pull Requests only against the **develop** branch of the repo.


### Setup Dev

SwellRT can be setup for eclipse and intellij IDE's.

Running `./gradlew eclipse` or `./gradlew idea` will generate all project files needed.
In a situation where dependencies have changed or project structure has changed
run `./gradlew cleanEclipse` or `./gradlew cleanIdea` depending on your IDE.


### Gradle Tasks

Java 8 and Gradle 2.8+ are required to build the project.

Gradle tasks can be run by:

```sh
$ ./gradlew [task name]
```


Build:

- **compileJava**: builds server.

- **devWeb**: compile Javascript Web client for development.
- **prodWeb**: compile Javascript Web client for production (optimized, minimized).

Test:

- **test**: runs the standard unit tests.

Run server:

- **run**: runs server. By default, Javascript client is at [http://localhost:9898/swellrt-beta.js](http://localhost:9898/swellrt-beta.js)

Debug server:

- **run --debug-jvm**: the server process will accept connection of remote debugger on port 5005.

Debug Web API:

- **debugWeb**: starts the [GWT dev mode](http://www.gwtproject.org/articles/superdevmode.html) to debug the Javascript client. Debug mode only works for one target browser, according to settings in _/wave/src/main/resources/org/swellrt/beta/client/ServiceFrontendDebug.gwt.xml_



Distribution Tasks:

- **jar**: builds jar file for the project.
- **sourcesJar**: builds a source jar file for each project.
- **createDist**: builds the zip and tar file for bin and source.
- **createDistBin**: builds the zip for distribution.
- **createDistBinZip**: builds the zip for distribution.
- **createDistBinTar**: builds the tar for distribution.
- **createDistSource**: builds the zip and tar file for distributing the source.
- **createDistSourceZip**: builds the zip for distributing the source.
- **createDistSourceTar**: builds the tar for distributing the source.



## Source code

SwellRT client's source code is written in Java/GWT-JsInterop. It is designed to target eventually...

- a JavaScript library to be used in Web (currently available)
- a GWT module be imported in GWT projects (currently available but not tested yet)

- a Java Android library (requires to replace platform dependent HTTP/Websocket libraries)
- a JavaScript library for NodeJs (requires to replace platform dependent HTTP/Websocket libraries)
- ideally, a Objective-C version using java2Objc (future plan)


Java Packages:

- **org.swellrt.beta** container for all Beta source code.

- **org.swellrt.beta.model**  SwellRT data model interfaces and common classes
- **org.swellrt.beta.model.local** implementation of data model interfaces, backed by client's data structures
- **org.swellrt.beta.model.remote** implementation of data model interfaces backed by Waves and Wavelets
- **org.swellrt.beta.model.js** Javascript/Browser specific binding classes (ES6 Proxies)

- **org.swellrt.beta.wave.transport** Wave client protocol classes related with transport. In general this is platform dependent code

- **org.swellrt.beta.client** Client API implementation. Shared interface for Java, GWT, and JS (JsInterop)
- **org.swellrt.beta.client.operation** Implementation of each API operation including HTTP
- **org.swellrt.beta.client.js** JsInterop Browser specific bindings  

Get more info about data model implementation in **org.swellrt.beta.model.remote.SObjectRemote**  Javadoc.  





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
