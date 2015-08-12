# SwellRT, a Real-Time Federated Collaboration Framework

SwellRT is a collaboration framework based on [Apache Wave](http://incubator.apache.org/wave/).
It allows to develop real-time collaborative apps for **Web** (JavaScript or GWT), **Android** and **Java**.

In a nutshell, SwellRT provides to apps shared objects that can be modified by different participants on nearly real-time
distributing changes and managing concurrency.

In particular, **text objects support collaborative real-time editing.**

You can install your own server infrastructure or build on top of an existing SwellRT provider.
SwellRT servers can be federated, so your app can be deployed in a decetralized way and become interoperable easily.

In this repo/file you will find info about the **JavaScript API** of SwellRT.


## Differences with the Wave Project

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


## Adding SwellRT to your Web App

A deep example of the JavaScript API can be found at https://github.com/P2Pvalue/swellrt-showcase

### Loading JavaScript library

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

### WebSockets vs. Long-Polling

By default, SwellRT's JavaScript client will communicate with the server using the **WebSocket** protocol if it is available. If not, the fallback protocol will be **long-polling**.
If you want to force disabling WebSockets, call the following method just before start a new session:

```
SwellRT.useWebSocket(false);
```

### Users

SwellRT apps will use the API in behalf of participants. Participants have an email-like address belonging to a specific server *e.g. @demo.swellrt.org*
The default domain in SwellRT local servers is *@local.net*. Please see the README.wave and original [Wave protocol](https://people.apache.org/~al/wave_docs/ApacheWaveProtocol-0.4.pdf) for more information.

Use the `registerUser()`method to create new accounts in a SwellRT server.


```
SwellRT.registerUser("http://demo.swellrt.org, "myuser", "password",

    function() {

        // Success Callback

        console.log("Created new user ");
    },

    function(error) {

        // Error Callback

        console.log("Error creating new user "+s);
     }

    );
```

In that example, the whole user account name will be *mysuer@demo.swellrt.org*. The API appends automatically the server's domain to each new user name.

### Sessions

Sessions are user-authenticated connections to a SwellRT server. API operations with collaborative data models must be performed within a session.

Start a session in the SwellRT Server providing the participant's credentials.
```
  SwellRT.startSession("http://demo.swellrt.org", "myuser", "password",

      function(sessionId) {

        // Success Callback

      }, function(error) {

        // Failure Callback

      });
};

```

To stop the session and to get disconected from the server use the `stopSession()` method.

```
 SwellRT.stopSession();
```

### Anonymous Sessions

To start an anonymous sessions use the user *SwellRT.user.ANONYMOUS* and empty password. The session will be valid during the browser session.

```
  SwellRT.startSession("http://demo.swellrt.org", SwellRT.user.ANONYMOUS, "", ...

```


### Handling Network Status

SwellRT requires a reliable network connection to work smoothly. Client apps must be aware of any network issue in order to
take control of the UI and provide effective feedback to the users.

The API allows to set different event handlers to let apps control network issues through a global handler register method:

```
SwellRT.on(SwellRT.events.<global_event>, <callback_function>(data));
```

**SwellRT.events.NETWORK_CONNECTED**

Event triggered on network new connection or reconnection. The client app can resume API operations.

**SwellRT.events.NETWORK_DISCONNECTED**

Event triggered on a temporary or total network disconnection. The app should prevent further calls to the API until a *NETWORK_CONNECTED* event is received.

**SwellRT.events.DATA_STATUS_CHANGED**

Event triggered on data changes performed by your app. It provides three status indicators each time your app makes changes to a data model:

`data.inFlightSize`:
* Number of changes made in the current opened data model but **not sent** to the server yet.

`data.uncommittedSize`:
* Number of changes made in the current opened data model, **already sent** to the server but **not commited in server storage** yet.


`data.unacknowledgedSize`:
* Number of changes made in the current opened data model **commited in the server without acknowledge**.

Any local changes should trigger eventually this event with all values equals to 0. This fact will confirm that all your changes are properly
stored and distributed among other participants.

Be aware of uncommited and unacknowledge changes: if a relative period of time goes by but a DATA_STATUS_CHANGED event with all values equals to zero doesn't occur,
probably a communication problem exists and the app should prevent further use of the API.

**SwellRT.events.FATAL_EXCEPTION**

Event triggered on fatal exception in the client or server. You should stop your app. You can check `data.cause` for more information.
The app should start a new session (and open a model) before resuming API operations.



## JavaScript API Guide


### Collaborative data models

Data models are created in a session context with an unique ID. Pass to the `createModel()` method both callback methods for success and
failure:

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

The return value of `createModel()` is the data model ID (a.k.a. Wave ID) and
it's required later to close the model properly. The user who creates a data model is the *author*.

You must keep a local reference to the `model` object passed to the *success* callback method. In this documentation we keep that reference as
a local attribute of the SwellRT global object: `SwellRT.model`.


To open an existing data model instance you must provide the ID and both callbacks:

```
        // We pass the ID as first parameter

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

Close the collaborative model instance using the provided ID. This method will dispose server's connection and resources:

```
    SwellRT.closeModel("local.net/dummy+Xxn3-XupCUA");
```


### The Data Model object

The model object is the data container shared among participants. The creator must add new participants. Any participant can add additional ones.

```
SwellRT.model.addParticipant("myfriend@local.net");
SwellRT.model.removeParticipant("myfriend@local.net");

```

Of course, a listener can be added to be notified when a new participant is added or an existing one is removed.

```
SwellRT.model.registerEventHandler(SwellRT.events.PARTICIPANT_ADDED, function(address) { ... });
SwellRT.model.registerEventHandler(SwellRT.events.PARTICIPANT_REMOVED, function(address) { ... });
```


### The Root Map object

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


### Observable Data objects

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

Handler functions receive operation's changes as parameter:
- String: two string parameters: **new value**, **old value**
- List: array of objects: **index**, **new value**, [**old value**]
- Map: array of objects: **key**, **new value / removed value**, [**old value**]




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
var observable_object = list.add(<observable object>, <index>);
var observable_object = list.get(<index>);
list.size();
list.remove(<index>);
```
### Text Documents

Text documents allow real-time collaborative text editing from different clients. They are based on the Apache Wave's Documents XML format (http://www.waveprotocol.org/protocol/draft-protocol-specs/wave-conversation-model).
Each XML's start/end tag is represented as a location point in the document. First location is 0.

Create a Text doc. object and attach it to the model before start editing it.

```
text = SwellRT.model.createText("Write here initial content");
SwellRT.model.root.put("text",text);
```

A document exposes following methods to change its content:

```
text.insert(2, "A new text"); // Insert text at location 2. Text can't contain XML.
text.delete(2,3);   // Delete one character starting at location 2.
text.newLine(3);    // Insert a new line.
text.size();        // Returns the size of the document, tags are counted as locations.
text.xml();         // Returns the XML as string.
```

Wave's documents supports annotations relative to a range of locations:

```
text.setAnnotation(10, 16, "key", "value"); // Set a new annotation spaning the location range.
text.setAnnotation(5, 20, "key", null); // Remove all annotations with the provided key within the location range.
text.getAnnotation:(12, "key"); // Get the annotation's value in the location.
text.getAllAnnotations(5, 20); // Get all annotations withinn the provided range.

```



### Text editor

First, create a text object and attach it to the model before editing:

```
text = SwellRT.model.createText("Write here initial content");
SwellRT.model.root.put("text",text);
```

In your Web page, provide a Text Editor and edit your Text objects:

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

### Searching data models
Users can query the server to look up data models following some conditions. Use the `SwellRT.query(query, onSuccess, onFailure)` method.
The *query* parameter can be both a string or a JavaScript object standing for a MondoDB query expresion.


```

// Get all data models accesible for the logged in user

SwellRT.query("{}",

              function(response) {

                // response.result is the array of data modeles

              },

              function(error) {

                // handle the error

              });

```

To use a [MongoDB projection](http://docs.mongodb.org/manual/tutorial/project-fields-from-query-results/#projection) in the query, simply call SwellRT method with an object with the following structure as the query param:

```
{ _query : <the query object>,
  _projecion: <the projection object>
}
```

Data models are represented as JSON objects:

```
{"result":[

 { "wave_id" : "local.net/s+SW-JnsVO14A" ,
   "participants" : [ "tom@local.net" , "tim@local.net"] ,
   "root" : {
        "list" : [ "String one" , "String two"]
            }
 },

 { "wave_id" : "local.net/s+Nfz-Pxo0e8A" ,
   "participants" : [ "tom@local.net"] ,
   "root" : {
        "list" : [ "Hello World" , "This" , "is an" , "array"]
            }
  }

```


### Checking object's data type

Use the method *type()* to check object's type.

```
SwellRT.model.root.type() == SwellRT.type.MAP;
```

### Avatars

A Gmail-like avatar generator is provided. It uses an array of user data to generate a list of HTML elements that can be added anywhere in the DOM.

Set up an array with users information. Each object can contain following attributes:
- **name**: string with the user name. The first letter will be used as default avatar.
- **pictureUrl**: URL to a picture of the  user. If provided, it will be used instead of the name.
- **additionalElement**: a DOM element to render as avatar. If provided, it will be used instead of others fields.

```
usersInfo = [
    {
        name: "first.user@demo.org",
        pictureUrl: "http://www.gravatar.com/avatar/d49414ee9e531c69427baf0ba2b76191?s=40&d=identicon"
    },
    {
        name: "second.user@demo.org",
        pictureUrl: "http://www.gravatar.com/avatar/6950571d68e7a5a0a4ede1b4e742cc79?s=40&d=identicon"
    },
    {
        name: "third.user@demo.org",
        pictureUrl: "http://www.gravatar.com/avatar/e28316e4c10e90c342ae5d07522485a7?s=40&d=identicon"
    },
    {
        name: "four.user@demo.org",
        additionalElement: <a DOM Element>
    },
    {
        name: "xive.user@demo.org",
    },
    {
        name: "six.user@demo.org",
    }
];
```

Generate an array of avatars as DOM elements calling

```
SwellRT.utils.avatar(usersInfo, size, padding, numberOfItems, cssClass);
```

- **usersInfo**: the array of user information described before.
- **size**: the size of the avatar box in pixels.
- **padding**: the internal padding of each avatar if it's a multiple avatar, in pixels.
- **numberOfItems**: number of avatars to be generated as DOM elements.
- **cssClass**: a CSS class to add in generated DOM elements.

The function generates  #*numberOfItems* avatars. If there are more users than items to generate,
the last avatar will be multiple, showing up to 4 avatars in the same box.


This feature is backed up by the Kune subproject https://github.com/comunes/gwt-initials-avatars.

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

