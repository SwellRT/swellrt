#WaveJS

Real-time collaboration Java Script API and federated server infraestructure.
This project is an extension of [Apache Wave](http://incubator.apache.org/wave/).


## Objective

The aim of WaveJS is to establish a handy framework to build applications on top of
the Wave technology, providing:

- A client as Java Script API to work with general collaborative data models.
- A federated server with customizable index engine to query collaborative data models in non-collaborative contexts.


## Differences with Wave Project

The original Wave project is both a...

- end-user Web tool for real-time communication and collaborative editing based on Conversations.
- distributed and federated protocol and server to run those Conversations across differents domains and servers (like email).

WaveJS generalizes the purpose of the original project. It presents the underlying capabilities (collaborative editing, federation...)
of Wave to build new end-user applications.


## What is a Collaborative Data Model?




## Quick use guide

1. Prerequisites

2. Setting up a Server

3. Using the JavaScript API


## Developing WaveJS


## Build Web Client Mod

A Web Client modification is provided to play with new types:

```
ant compile

ant compile-gwt-mod
```

Ant targets for Hosted and Superdev mode are also provided.


## Build WaveJS Library


```
ant compile

ant compile-wavejs

or

ant compile-wavejs-dev
```


## Run WaveJS API End-to-End tests

These tests need a running instance of the Wave Mod Server.
```
ant run-server
```

Go to `http://localhost:9898/test/wavejs.html`. It provides a Web console to launch WaveJS API tests.
It also shows how to use the API.


## Debug WaveJS API (Requires a GWT compatible broswer)

```
ant run-server

ant hosted-wavejs
```

Go to `http://localhost:9898/test/wavejs.html?gwt.codesvr=localhost:9997`

WaveJS API is a GWT app. More info about GWT compiling and debugging here:
http://www.gwtproject.org/doc/latest/DevGuideCompilingAndDebugging.html


## Using the WaveJS API in your Web site


### Preconditions

* Access to a Wave Mod Server. By default the Wave Server runs at http://localhost:9898.
* An user account. Navigate to http://localhost:9898 and follow the instructions.

See the README file to build and run the Wave Server.


### Import JavaScript WaveJS Client Library

```
<script type="text/javascript" src="http://localhost:9898/wavejs/wavejs.nocache.js"></script>
```

The following method is called when library is fully loaded, from here the WaveJS object is available.

```
  function onWaveJSReady() {
    // Here your code using the WaveJS API:
    // WaveJS.startSession(...
    // WaveJS.open...
    // ...
  }
```


### Start and Stop sessions

In order to work with any collaborative feature you must start a session against the Wave Server with an user's credentials.

```
  WaveJS.startSession("http://localhost:9898", "MyUserName@waveDomain", "MyPassword",
      function(sessionId) {

        // Success Callback


      }, function(error) {

        // Failure Callback

      });
};

```

To stop a session (it disposes resources and connections to the server) use following method:

```
 WaveJS.stopSession();
```


### Create and use a collaborative data model


Create the model instance

```
    // Store the modelId to retrive the data in future sessions
    var modelId =
        WaveJS.createModel(

           function(model) {

                // Attach the created model instance to the WaveJS object
                // Just for demostration
                WaveJS.model = model;

                // Add a participant to participate in the data model
                WaveJS.model.addParticipant("myfriend@local.net");

                // Get the root map of the collaborative model
                var root = WaveJS.model.root;

                // ...
            },

            function(error) {

              alert("Error creating collaborative data model instance "+error);

            });
```

Open an existing model instance

```
        // We pass the Id as first parameter

        WaveJS.openModel("local.net/dummy+Xxn3-XupCUA",

           function(model) {

                // Attach the opened model instance to the WaveJS object
                // Just for demostration
                WaveJS.model = model;

                // ...

            },

            function(error) {

              alert("Error openning collaborative data model instance "+error);

            });
```

Close the collaborative model instance, it disposes connection and resources:

```
    WaveJS.close("local.net/dummy+Xxn3-XupCUA");
```


