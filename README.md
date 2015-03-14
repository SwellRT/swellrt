# Real-time collaboration API for Wave

Real-time collaboration Java Script API and federated server infraestructure.
This project is an extension of [Apache Wave](http://incubator.apache.org/wave/).


## Objective

The aim of this API is to establish a handy framework to build applications on top of
the Wave technology, providing:

- A JavaScript API to work with general collaborative data models.
- A Wave federated server with customizable index engine to query collaborative data models in non-collaborative contexts.


## Differences with Wave Project

The original Wave project provides...

- an end-user Web tool for real-time communication and collaborative editing based on Conversations.
- a distributed and federated protocol and server to run those Conversations across differents domains and servers (like email).

This API generalizes the purpose of the original project. It allows to use the underlying capabilities of Wave (collaborative editing, federation...)
in new end-user applications, either Web apps (HTML + JavaScript) or Java. It could work on Android also.


## What is a Collaborative Data Model?

A collaborative data model is a data structure that is shared between a set of participants or users.
Any change made in the model by a participant can be notified to any other participant on near real-time.

Data models are formed by a free combination of
- Maps and lists
- String values

The data structure can be changed any time. The API provides a root Map which contains the rest of structures and data.


## Setting up Wave API

### Prerequisites

Minimun:
- Java JDK 6
- Apache Ant
- GWT 2.5

Recomended:
- MongoDB


### Build Server and API


Clone the Wave API server to a local forlder. Then `cd` to it. Wave uses Ant to build.

Get Apache Wave Dependencies

```
ant get-third-party
```


Build the server
```
ant compile
```

Build the current Wave UI with some quirks (it's needed to create users at this moment).
Ant targets for Hosted and Superdev mode are also provided. See the ant `build.xml` file.
```
ant compile-gwt-mod
```

Build the JavaScript API client for development
```
ant compile-wavejs-dev
```

To compile the Javascript API client for production use `ant compile-wavejs`.


### Configure the Server

You need to configure your Wave server instance before you can use it. To create a default simple configuration for the first time run:
```
ant -f server-config.xml
```

Then, you can edit the server configuration in the generated `server.config` file.

### Run the Server

The JavaScript API client is served by the Wave server itself. Start it with following task:
```
ant run-server
```

The server will be ready after following message is prompted:

```
 [java] 19069 [main] INFO org.eclipse.jetty.server.ServerConnector - Started ServerConnector@ae26011{HTTP/1.1}{localhost:9898}
```

For more info please check `README.wave`

### Users

In order to work with data models, the API requires be accesed with a participant account.
To create a participant go to `http://localhost:9898`.
You will be prompted to login into the current Apache Wave UI or to register a new account. Follow that link or go to:

```
http://localhost:9898/auth/register
```

By default, the server is assigned to the Wave domain `local.net`.


### Using the JS API

#### Import the JS library

Add the following script tag. Take into account the Wave server host:
```
<script type="text/javascript" src="http://localhost:9898/wavejs/wavejs.nocache.js"></script>
```

The following method will be called when library was fully loaded. Then the WaveJS object will be available.

```
  function onWaveJSReady() {
    // Here your code using the API
    // WaveJS.startSession(...
    // ...
  }
```


#### Start and Stop sessions

In order to work with the API, first you must start a session against the Wave Server with a participant's credentials.
```
  WaveJS.startSession("http://localhost:9898", "myuser@local.net", "mypassword",
      function(sessionId) {

        // Success Callback


      }, function(error) {

        // Failure Callback

      });
};

```

To stop the session and to get disconect from the server. Be carefull, any change in the data model is

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

Don't forget to store the return value of WaveJS.createModel(...). It's the model instance Id (a.k.a. wave Id).
It's required to close the model later.


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

Close the collaborative model instance, it closes server's connection and dispose resources:

```
    WaveJS.closeWave("local.net/dummy+Xxn3-XupCUA");
```


### Test playground


Go to `http://localhost:9898/test/wavejs.html`. It provides a Web console to launch WaveJS API tests.
It also allows to open and to create data models.



### Debugging the API

In order to debug the API (like any other GWT app) start a Hosted session as follows
```
ant hosted-wavejs
```

Go to `http://localhost:9898/test/wavejs.html?gwt.codesvr=localhost:9997` using a GWT compatible browser (e.g. Firefox 23)

More info about GWT compiling and debugging here:
http://www.gwtproject.org/doc/latest/DevGuideCompilingAndDebugging.html


## Using the Wave API

This section assumes you have already opened a session in your Web App and you have attached the opened model to `WaveJS.model`, as it's shown in the previous section.

### The model object

The model is the base object that can be shared between participants. A first user creates it and then it's shared adding participants:

```
WaveJS.model.addParticipant("myfriend@local.net");
WaveJS.model.removeParticipant("myfriend@local.net");

```

Of course, a listener can be added to be notified when a new participant is added or an existing one is removed.

```
WaveJS.model.registerEventHandler(WaveJS.events.PARTICIPANT_ADDED, function(address) { ... });
WaveJS.model.registerEventHandler(WaveJS.events.PARTICIPANT_REMOVED, function(address) { ... });
```


### The root map

Any data model provide by default a root Map where data structures and values will be attached.

To add a new string to the root map:
```
var string_key1 = WaveJS.model.root.put("key1","hello world");
```

The 'put()' method returns an observable String object. To get the actual string value:
```
alert(string_key1.getValue());
```

To get the keys on the map as array:
```
alert(WaveJS.model.root.keySet());
```


### Observable objects

Objects in the data model are observables. This allows you to register listeners to know when the object changes.
For example, to handle changes in a string value:
```
string_key1.registerEventHandler(WaveJS.events.ITEM_CHANGED,

                                 function(oldStr, newStr) {

                                    alert("String changed:"+oldStr+"->"+newStr);

                                 });
```

All model objects (maps, lists and strings) provides these two methods:

```
observableObject.registerEventHandler: function(event, handler);
observableObject.unregisterEventHandler: function(event, handler);
```
The 'event' parameter is a value from the `WaveJS.events` object. 'handler' is a listener function. It is called when the event occurs.
A list of events applying each observable type follows:
- String: `WaveJS.events.ITEM_CHANGED`
- Map: `WaveJS.events.ITEM_ADDED` `WaveJS.events.ITEM_CHANGED` `WaveJS.events.ITEM_REMOVED`
- List: `WaveJS.events.ITEM_ADDED` `WaveJS.events.ITEM_REMOVED`


### Using observable objects

Observable objects are created from the 'model' object:
```
var list = WaveJS.model.createList();
var map = WaveJS.model.createMap();
var str = WaveJS.model.createString("default value");
```

They are not useful until they are attached to the model, for example, being added to the root map, or added to an already attached object:
```
map = WaveJS.model.root.put("keymap", map); // map is attached to the root
list = map.put("keylist",list); // list is attached to the sub map: root->map->list
str = list.add(str); // root->map->list(0)->str
```

If you try to attach an object to a non already attached object an exception is thrown:
```
java.lang.IllegalArgumentException: ListType.add(): not attached to model
```

### Strings

Strings provide following methods:
```
str.setValue("string value");
str.getValue();
```


### Maps

Maps provide following methods:
```
var observable_object = map.put("key",<observable object>);
var observable_object = map.get("key");
var keyArray = map.keySet();
map.remove("key");
```

### Lists

Lists provide folliwing methods:
```
var observable_object = list.add(<observable object>);
var observable_object = list.get(<index>);
list.size();
list.remove(<index>);
```

### General API callbacks

The API runtime will call to some predefined methods in following cases:

`window.onWaveJSException(exception)` will be invoked if some serious exception happens. You must avoid further actions to the current Wave Content instance and close it.

`window.onWaveJSUpdate(inFlightSize, notAckedSize, unCommitedSize)` will be invoked anytime you have perfomed actions to the current Wave Content instance. Having any parameter
with value different to 0 implies that changes are not persisted in the server yet. This method is invoked anytime unsaved data is acknowledge or commited by the server.

`window.onWaveJSClose(everythingCommitted)` will be invoked if connection to server for current Wave Content is closed. You must avoid further actions
to the current Wave Content instance. Closing the wave instance is not necessary.



