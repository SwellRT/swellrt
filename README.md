# SwellRT, a Real-Time Federated Collaboration Framework

This is a collaboration framework based on [Apache Wave](http://incubator.apache.org/wave/).
It allows to write real-time collaborative apps for **Web** (JavaScript or GWT), **Android** and **Java**.

In a nutshell, SwellRT provides to apps shared objects that can be modified by different participants on nearly real-time
distributing changes and managing concurrency.

In particular, **text objects support collaborative real-time editing.**

You can install your own server infrastructure or build on top of an existing SwellRT provider.
SwellRT servers can be federated, so your app can be deployed in a decetralized way and become interoperable easily.

In this repo/file you will find info about the **JavaScript API** of SwellRT.
For Android clients, please visit this repo.


## Differences with Wave Project

The original Wave project provides...

- an end-user Web tool for real-time communication and collaborative editing based on Conversations.
- a distributed and federated protocol and server to run those Conversations across differents domains and servers (like email).

SwellRT generalizes the purpose of the original project. It allows to use the underlying capabilities of Wave (collaborative editing, federation...)
in your own applications.


## What is a Collaborative Data Model?

A collaborative data model is a data structure that is shared between a set of participants or users.
Any change made in the model by a participant is notified to others on near real-time.

Data models are formed by a free combination of
- Maps and lists
- String values
- Text documents

The data structure can be changed at any time. The API provides a root Map which contains the rest of data elements.

## Setting up a SwellRT Server (JavaScript API Provider)

### Fastest way: use Docker

1. [Install Docker](https://docs.docker.com/installation/)

2. sudo docker run -p 9898:9898 -h swellrt -d p2pvalue/swellrt

### Hardcore way: Setting up your environment

#### Prerequisites

Minimun:
- Java JDK 6
- Apache Ant
- GWT 2.5

Recomended:
- MongoDB


#### Build and Run

Clone this repo, go into the created folder and follow these steps:


1. Get Third Party Dependencies:

  `ant -f build-swellrt.xml get-third-party`

2. Build server:

  `ant -f build-swellrt.xml compile`

3. Build the former Wave's Web Client:

  `ant -f build-swellrt.xml swellrt-webclient-compile`

4. Build the JavaScript API client

  * For development: `ant -f build-swellrt.xml swellrt-js-compile-dev`
  * For production:  `ant -f build-swellrt.xml swellrt-js-compile`


#### Configure the Server

You need to configure your SwellRT/Wave server instance before you can use it. To create a default simple configuration for the first time run:

* `ant -f server-config.xml`.

Then, you can edit the server configuration in the generated `server.config` file.

#### Run the Server

The JavaScript API client is served by the Web server. Start it with following task:

* `ant run-server`

The server will be ready after following message is prompted:
`
 [java] 19069 [main] INFO org.eclipse.jetty.server.ServerConnector - Started ServerConnector@ae26011{HTTP/1.1}{localhost:9898}
`

For more info please check `README.wave`

### Users

In order to work with shared objects/data models, your app must access the server in behalf of a participant.
To create a participant go to `http://localhost:9898`.
You will be prompted to login into the Apache Wave UI or to register a new account. Follow that link or go to:

* `http://localhost:9898/auth/register`

By default, the server is assigned to the domain `local.net`.


### Using SwellRT JavaScript API

A deep example of the JavaScript API can be found at https://github.com/P2Pvalue/swellrt-showcase

#### Setting up the JS library

Add the js client in your web page.
```
<script type="text/javascript" src="http://localhost:9898/swellrt/swellrt.nocache.js"></script>
```

The `onSwellRTReady()` method will be called when js file was fully loaded. Then the SwellRT object will be available.

```
  function onSwellRTReady() {
    // Here your code using the API
    // SwellRT.startSession(...
    // ...
  }
```

#### WebSockets vs. Long-Polling

By default, SwellRT's JavaScript client will communicate with the server using the **WebSocket** protocol if it is available. If not, the fallback protocol will be **long-polling**.
If you want to force disabling WebSockets, call the following method just before start a new session:

```
SwellRT.useWebSocket(false);
```

#### Start and Stop sessions

In order to work with collaborative objects, first you must start a session in the Wave Server with a participant's credentials.
```
  SwellRT.startSession("http://localhost:9898", "myuser@local.net", "mypassword",
      function(sessionId) {

        // Success Callback


      }, function(error) {

        // Failure Callback

      });
};

```

To stop the session and to get disconect from the server. Be carefull, any change in the data model is

```
 SwellRT.stopSession();
```


### Create and use a collaborative data model


Create the model instance:

```
    // Store the modelId to retrive the data in future sessions
    var modelId =
        SwellRT.createModel(

           function(model) {

                // Attach the created model instance to the SwellRT object
                // Just for demostration
                SwellRT.model = model;

                // Add a participant to participate in the data model
                SwellRT.model.addParticipant("myfriend@local.net");

                // Get the root map of the collaborative model
                var root = SwellRT.model.root;

                // ...
            },

            function(error) {

              alert("Error creating collaborative data model instance "+error);

            });

```

Don't forget to store the return value of SwellRT.createModel(...). It's the model instance Id (a.k.a. wave Id).
It's required to close the model later.


Open an existing model instance

```
        // We pass the Id as first parameter

        SwellRT.openModel("local.net/dummy+Xxn3-XupCUA",

           function(model) {

                // Attach the opened model instance to the SwellRT object
                // Just for demostration
                SwellRT.model = model;

                // ...

            },

            function(error) {

              alert("Error openning collaborative data model instance "+error);

            });
```

Close the collaborative model instance, it closes server's connection and dispose resources:

```
    SwellRT.closeModel("local.net/dummy+Xxn3-XupCUA");
```

## Using the SwellRT API

This section assumes you have already opened a session in your Web App and you have attached the opened model to `SwellRT.model`, as it's shown in the previous section.

### The model shared object

The model is the base object that can be shared between participants. A first user creates it and then it's shared adding participants:

```
SwellRT.model.addParticipant("myfriend@local.net");
SwellRT.model.removeParticipant("myfriend@local.net");

```

Of course, a listener can be added to be notified when a new participant is added or an existing one is removed.

```
SwellRT.model.registerEventHandler(SwellRT.events.PARTICIPANT_ADDED, function(address) { ... });
SwellRT.model.registerEventHandler(SwellRT.events.PARTICIPANT_REMOVED, function(address) { ... });
```


### The root map

Any data model provide by default a root Map where data structures and values will be attached.

To add a new string to the root map:
```
var string_key1 = SwellRT.model.root.put("key1","hello world");
```

The 'put()' method returns an observable String object. To get the actual string value:
```
alert(string_key1.getValue());
```

To get the keys on the map as array:
```
alert(SwellRT.model.root.keySet());
```


### Observable objects

Objects in the data model are observables. This allows you to register listeners to know when the object changes.
For example, to handle changes in a string value:
```
string_key1.registerEventHandler(SwellRT.events.ITEM_CHANGED,

                                 function(newStr, oldStr) {

                                    alert("String changed:"+oldStr+"->"+newStr);

                                 });
```

All model objects (maps, lists and strings) provides these two methods:

```
observableObject.registerEventHandler: function(event, handler);
observableObject.unregisterEventHandler: function(event, handler);
```
The 'event' parameter is a value from the `SwellRT.events` object. 'handler' is a listener function. It is called when the event occurs.
A list of events applying each observable type follows:
- String: `SwellRT.events.ITEM_CHANGED`
- Map: `SwellRT.events.ITEM_ADDED` `SwellRT.events.ITEM_CHANGED` `SwellRT.events.ITEM_REMOVED`
- List: `SwellRT.events.ITEM_ADDED` `SwellRT.events.ITEM_REMOVED`


### Using observable objects

Observable objects are created from the 'model' object:
```
var list = SwellRT.model.createList();
var map = SwellRT.model.createMap();
var str = SwellRT.model.createString("default value");
var txt = SwellRT.model.createText("initial content");
```

They are not useful until they are attached to the model, for example, being added to the root map, or added to an already attached object:
```
map = SwellRT.model.root.put("keymap", map); // map is attached to the root
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
### Texts & Web Text editor

Text objects allows collaborative editing of formatted text through the provided *Text Editor*.

Create a text object and attach it to the model before editing:

```
text = SwellRT.model.createText("Write here initial content");
SwellRT.model.root.put("text",text);
```

In your Web, provide a Text Editor and edit your Text objects:

```
<!-- The element where the editor will be displayed -->
<div id="editor-panel"/></div>


<script>

// Create an editor associated to an existing DOM element
editor = SwellRT.editor("editor-panel");

// Start editing a text object from your shared model
editor.edit(SwellRT.model.root.get("text"));

// At this point, the text is displayed and it can be edited.

// Dispose the editor before editing other text
editor.cleanUp();

// At this point, the text is not displayed.

</script>

```

### Checking object's data type

Use the method *type()* to check object's type.

```
SwellRT.model.root.type() == SwellRT.type.MAP;
```

### General API callbacks

The API runtime will call to some predefined methods in following cases:

`window.onSwellRTException(exception)` will be invoked if some serious exception happens. You must avoid further actions to the current Wave Content instance and close it.

`window.onSwellRTUpdate(inFlightSize, notAckedSize, unCommitedSize)` will be invoked anytime you have perfomed actions to the current Wave Content instance. Having any parameter
with value different to 0 implies that changes are not persisted in the server yet. This method is invoked anytime unsaved data is acknowledge or commited by the server.

`window.onSwellRTClose(everythingCommitted)` will be invoked if connection to server for current Wave Content is closed. You must avoid further actions
to the current Wave Content instance. Closing the wave instance is not necessary.



# Developing SwellRT

## Packages

| Package               |                              |                        |
|-----------------------|------------------------------|------------------------|
|org.swell.model        | Generic Wave Data Model      | Java & GWT             |
|org.swell.client       | Wave Protocol Client mods    | GWT                    |
|org.swell.webclient    | Wave UI Client mods          | GWT                    |
|org.swell.api          | JS API Generic Data Model    | GWT                    |
|org.swell.server       | Wave Java server mods        | Java                   |


### Debug (GWT Super Dev Mode)

First, launch the web server `ant run-server` serving the SwellRT javascript files.

Enable debugging of SwellRT/Apache Wave source code starting a **GWT Super Dev** session:

```
ant -f build-swellrt.xml swellrt-js-superdev
```

Then (by default) visit `http://localhost:9876` and bookmark provided links for de/activate the Dev mode.

Go to your web (e.g. http://localhost:9898/test/index.html) and activate de Dev mode with the provided link.

You can now use the browser's debugger as usual. Please, be sure your browser's debbuger has the "source maps" option activated.
Chrome is recommended.

For more info about GWT debugging, please visit http://www.gwtproject.org/articles/superdevmode.html


#### Debug issues

Debugging in the old hosted mode could raise the following error:

```
00:10:30,530 [ERROR] Failed to load module 'swellrt' from user agent 'Mozilla/5.0 (X11; Linux x86_64; rv:23.0) Gecko/20100101 Firefox/23.0' at localhost:36570
java.lang.AssertionError: Jso should contain method: @org.waveprotocol.wave.model.adt.ObservableElementList.Listener::onValueAdded(Ljava/lang/Object;)
at com.google.gwt.dev.shell.CompilingClassLoader$MyInstanceMethodOracle.<init>(CompilingClassLoader.java:431)
...
```
It can be avoided removing the **final** modifier from overrided methods in the **ObservableListJS** class.

