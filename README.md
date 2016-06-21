# SwellRT

SwellRT is a **real-time storage platform**. This project includes the server runtime and the JavaScript client for Web applications.

SwellRT enables **real-time collaboration** in your Web applications: multiple users can share and edit JavaScript objects in real-time with transparent conflict resolution (*eventual consistency*). Changes are distributed in real-time to any user or App instance using a shared object.

Objects are stored in the server and can be query using the API.

SwellRT provides also **out-of-the-box collaborative rich-text editing** for Web applications (as Google Docs (R) or Etherpad) through an extensible **text editor Web component and API**.

## Getting Started

These instructions will get you the latest version of the server up and running in your machine.

### Prerequisites 

- [Java JDK 8](http://openjdk.java.net/install/)
- [MongoDB 2.4](https://www.mongodb.com/download-center#community)

### Install and run

1. Download latest binary release (tar or zip) from [GitHub](https://github.com/P2Pvalue/swellrt/releases/latest)

2. Extract server binary files
``tar zxvf swellrt-bin-X.Y.Z.tar.gz``

3. Run the server
``swellrt/run-server.sh`` (Linux)
``swellrt/run-server.bat`` (Windows)

4. Check out demo app
`http://localhost:9898/demo`

### Server configuration

Default configuration is provided in the file ``swellrt/config/reference.conf``. 
To overwrite a property, do create a new file named ``swellrt/config/application.config`` and put there the property with the new value.

## Documentation

For SwellRT development guide and API reference please visit our [Wiki](https://github.com/P2Pvalue/swellrt/wiki)


## Contributing

### Getting the source

Clone the SwellRT source code from the GitHub repo:
`git clone git@github.com:P2Pvalue/swellrt.git`

### Setup Dev

SwellRT can be setup for eclipse and intellij IDE's.

Running `./gradlew eclipse` or `./gradlew idea` will generate all project files needed.
In a situation where dependencies have changed or project structure has changed
run `./gradlew cleanEclipse` or `./gradlew cleanIdea` depending on your IDE.


### Gradle Tasks

Java 8 & Gradle 2.8+ is required to build the project

Gradle tasks can be run by `./gradlew [task name]`

Test Tasks:

- **test**: runs the standard unit tests.
- **testMongo**: runs the mongodb tests.
- **testLarge**: runs the more lengthy test cases.
- **testAll**: runs all the above tests.

Compile Tasks:

- **generateMessages**: Generates the message source files from the .st sources.
- **generateGXP**: Compiles sources from the gxp files.
- **compileJava**: Compiles all java sources.
- **compileJsWebDev**: Compiles all the Gwt sources for development (Javascript Web client)
- **compileJsWeb**: Compiles all the Gwt sources. (Javascript Web client)


Run Tasks:

- **run**: runs the server with the default parameters.
- **gwtDev**: runs the gwt development mode to debug Javascript Web client.

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


### Build

To build the client and server:
    `./gradlew jar`
It will be created in wave/build/libs/wave-*version*.jar

The sources can also be packaged into a jar by doing
    `./gradlew sourcesJar`
This will create a `project name`-sources.jar in each projects build/libs directory.

Note:

- if pst-`version`.jar is unable to be found run `./gradlew pst:jar` then retry.
- if a jar is unable to be unzipped with wave:extractApi then delete the jar from your cache and try again.
    You may need to restart.

Take a look at the reference.conf to learn about configuration and possible/default values.

The server can be started (on Linux/MacOS) by running
    ./run-server.sh
Or on Windows by running
    run-server.bat
    Note: must be cd'ed into the root directory
Or, you can run the server from the compiled classes with Gradle:
    gradle run
The web client will be accessible by default at http://localhost:9898/.



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




