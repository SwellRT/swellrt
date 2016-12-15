![](http://php-carbonero.rhcloud.com/swellrt/wp-content/uploads/sites/3/2016/08/Logo-SwellRT-tamanÌos-13.png)

SwellRT is a **real-time storage platform**. This project includes the server runtime and the JavaScript client for Web applications.

SwellRT enables **real-time collaboration** in your Web applications: multiple users can share and edit JavaScript objects in real-time with transparent conflict resolution (*eventual consistency*). Changes are distributed in real-time to any user or App instance using a shared object.

Objects are stored in the server and can be query using the API.

SwellRT provides also **out-of-the-box collaborative rich-text editing** for Web applications (as Google Docs® or Etherpad) through an extensible **text editor Web component and API**.

## NOTICE:

This README_BETA.md file refers only to the **Beta** source code of SwellRT. 

SwellRT **Alpha** source code is not going to be developed anymore. For more information about Alpha versions check out [README.md](README.md)

The source code of both versions lives together in this same Git repo/branch.  


## Roadmap Beta version

**Provide similar features than Alpha version**

- Re-implement all API operations in the current Alpha version, keeping input/output formats if possible.
- Add List data type
- Add mutation event bindings in data types
- Re-implement server's indexing events according to new data model implementation
- Re-implement server's event hub according to new data model implementation

**Additional features than Alpha version**

TBD

## Quick start

### Build

Clone the project

```
// From SwellRT repo
git clone git@github.com:P2Pvalue/swellrt.git
cd swellrt
git checkout master

//
// or
//

// From Apache Wave repo
git@github.com:apache/incubator-wave.git 
cd incubator-wave
git checkout swellrt

```

Build client and server

```
./gradlew compileJava compileWebDevBeta
```

Start the server

```
./gradlew run
```

Add JS client's script in your web project:

```
<script src='http://localhost:9898/swellrt-beta.js'></script>
```

or go to http://localhost:9898/swellrt-beta.html

Open the debugger console of the browser. 

### Using the API


**Get API instance**
```
var s = swellrt.service.get();
```


**Create user**
```
s.createUser({

    id: "ann",
    password: "ann",
    email: "ann@swellrt.org",
    locale: "en_EN",

}).then(r=>{ console.log("User created"); _response = r; })
.catch(e=>{ console.log("User creation error"); _exception = e; });

```

**Login**
```
s.login({ 

 id : "ann@local.net",
 password : "ann" 

}).then(r=>{ console.log("Login Successful"); _response = r; })
.catch(e=>{ console.log("Login error"); _exception = e; });
```

**Create / Load object**

Leave id field empty to create an object with an auto generated id.


```
s.open({

    id : "local.net/s+T6Ad2s2TC2A"

}).then(r=>{ console.log("Object opened"); _response = r; })
.catch(e=>{ console.log("Object could't be opened"); _exception = e; });
```

**Working with objects**


Using objects with Java map syntax
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
// Get the JS object view
jso = obj.asNative();

// Reading properties
jso.address.street;

// Adding properties
jso.address.state = "Hawaii";


// Adding nested map - as mutable properties

jso.quiver = swellrt.Map.create();
jso.quiver.put("surfboard-1-size", "6.1, 18 1/2, 3 1/4");
jso.quiver.put("surfboard-2-size", "5.11, 19 , 2 3/4");


// Adding nested map - as static js
// the whole JS object is stored as a single item,
// changes in properties won't throw events. 

jso.prize = {
	contest: "Fiji Pro",
	year: "2015",
	points: 12000
};


``` 



### Source code quick guide

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





## Getting Startedd

These instructions will get you the latest version of the server up and running in your machine.

### Prerequisites

- [Java JDK 8](http://openjdk.java.net/install/)
- [MongoDB 2.4](https://docs.mongodb.com/manual/administration/install-community/)

### Install and run

1. Download latest binary release (tar or zip) from [GitHub](https://github.com/P2Pvalue/swellrt/releases/latest).

2. Extract server binary files with ``tar zxvf swellrt-bin-X.Y.Z.tar.gz`` .

3. Start the server from the swellrt folder 
   
   Linux:
    ```
    cd swellrt 
    ./run-server.sh
    ``` 
    
    Windows:    
	 ```
	 cd swellrt
	 run-server.bat
	 ```
  
   Check out server status in ``http://demo.swellrt.org/test/``.
   Log in with user name 'test' and password 'test'.

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


### Server configuration

Default configuration is provided in the file [reference.conf](https://github.com/P2Pvalue/swellrt/blob/master/wave/config/reference.conf).
To overwrite a property, do create a new file named `application.config` in the config folder and put there the property with the new value.


### Contact and Support

Visit our [Gitter Channel](https://gitter.im/P2Pvalue/swellrt).

## Contributing

### Getting the source

Clone the SwellRT source code from the GitHub repo:

```sh
$ git clone https://github.com/P2Pvalue/swellrt
```

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

Test Tasks:

- **test**: runs the standard unit tests.
- **testMongo**: runs the mongodb tests.
- **testLarge**: runs the more lengthy test cases.
- **testAll**: runs all the above tests.

Build Tasks: 

- **compileJava**: builds server.
- **compileWebDevBeta**: builds the JavaScript client (for Web).

Run server Tasks:

- **run**: runs server. By default, Javascript client is at [http://localhost:9898/swellrt-beta.js](http://localhost:9898/swellrt-beta.js)

- **runWebServer**: runs a lightweight Web server to serve Javascript client at [http://localhost:8080/swellrt/swellrt-beta.js](http://localhost:8080/swellrt/swellrt-beta.js)

- **gwtDev** (To be updated for Beta): runs the [gwt super development mode](http://www.gwtproject.org/articles/superdevmode.html) to debug the JS client  library. Super dev mode only works for one target browser, according to settings in *org.swellrt.beta.client.ServiceFrontendDev.gwt.xml*



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


### Debugging server

Server can be debugged launching the *run* gradle task with following parameters:

```./gradlew run --debug-jvm```

The Java process will get suspended for the remote debugger connection on the port 5005.

### Build and Run redistributable package

To build the client and server:

```sh
$ ./gradlew jar
```

It will be created in `wave/build/libs/wave-*version*.jar`.

The sources can also be packaged into a jar by doing:

 ```sh
 $ ./gradlew sourcesJar
 ```

This will create a *project name*-sources.jar in each projects build/libs directory.

Note:

- if pst-*version*.jar is unable to be found run `./gradlew pst:jar` then retry.
- if a jar is unable to be unzipped with wave:extractApi then delete the jar from your cache and try again.
    You may need to restart.

Take a look at the reference.conf to learn about configuration and possible/default values.

The server can be started (on Linux/MacOS) by running `./run-server.sh` or on Windows by running `run-server.bat`. *Note: must be cd'ed into the root directory*.

Or, you can run the server from the compiled classes with Gradle: `gradle run`.

The web client will be accessible by default at <http://localhost:9898/>.



## Acknowledgments

The SwellRT project is based on [Apache Wave](http://incubator.apache.org/wave/).
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
