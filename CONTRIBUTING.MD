
# Constributing to SwellRT

First of all, thank you for taking out your time to contribute! 

## Pull Request Process

Pull Requests only against the **develop** branch of the repo. Clone the SwellRT source code from the GitHub repo:

```sh
$ git clone https://github.com/P2Pvalue/swellrt
```


## Setup Dev

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