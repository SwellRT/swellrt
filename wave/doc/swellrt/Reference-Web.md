
This section describes all methods in the Web API of Swell.

* [Loading Swell Javascript library](#loading-swell-javascript-library)
* [Users](#users)
* [Sessions](#sessions)
* [Collaborative Objects](#collaborative-objects)
* [Handling connection status](Handling connection status)
* [Search](#search)


### Loading Swell Javascript library


In your html file, load the `swell.js` script from your Swell server: 

```html
<script src="http://localhost:9898/swell.js"></script>
```

Then, register a callback to get notified when the script was fully loaded so you can start using the swell API afterwards:

```js
  swell.onReady( (service) => {
   
    // app logic here

   });
```

**API's entry point**

The *swell* global variable is set in the *window* object by the *swell.js* script. However it must be thought more like a namespace for Swell library and not as an entry poiny for the API.

The actual object containing the methods of the API is the one passed to the *onReady()*'s callback, in the example, the *service* variable. Keep this reference in your code for further use of the API.

In rest of this documentation we will use the name *service* to refer to the API's entry point.

You also can get this reference to the API's entry point with the following global method:

```js
  var service = swell.runtime.get();
``` 


**Promises and Callbacks**

The defaults API's entry point got from the *onReady()*'s callback (*service*), provides the API methods with **promises** as return values:

```js
    service.login({
      id : swell.Constants.ANONYMOUS_USER_ID,
      password : ''
    })
    .then( user => {  })
    .catch( error => {  });
```


In case you rather work with callbacks you can get the callback-based entry point of the API with the global method:

```js
  var callbackBasedService = swell.runtime.getCallbackable();
```

All API methods follow the same syntax for callbacks, for example

```js
    callbackBasedService.login({
      id : swell.Constants.ANONYMOUS_USER_ID,
      password : ''
    },

    {  
       onSuccess: function(user) {  },
       onError: function(error) {  }  
    });
```



### Users

SwellRT users are identified by a email-like strings where the domain part is the domain of a SwellRT server, for example:

*alice@swell.company.com*

All methods expecting an user id will admit just the *name* part (before the @), assuming she belongs to the current SwellRT domain. 

In federated installations of SwellRT, is recommended to use full id syntax to avoid issues.




#### createUser()

Creates a new user with the provided profile's information.

```js
service.createUser({

    id: 'tom',                // no add @domain part
    name: 'Tom Major'         // 
    password: '*****',        //
    email: 'tom@company.com', // (Optional)
    locale: 'en_EN',          // (Optional)
    avatarData:               // (Optional) jpg or png in base64 encoded string

})
.then( profile => {  })
.catch( error => {  });
```



#### editUser()

Edits the profile of the user currently logged in.
To delete the value of an attribute, add it to the request with an empty value.

```js
service.editUser({

    id: 'tom@swell.company.com', // use or nor your swell domain here.    
    name: 'Tomas Major '         // changing the name
})
.then( profile => {  })
.catch( error => {  });
```

#### getUser()

Retrieves profile information of one single user.


```js
service.getUser({

    id: 'tom@swell.company.com', // use or nor your swell domain here.               
})
.then( profile => {  })
.catch( error => {  });
```


#### getUserBatch()

Retrieves profile information of a set of users. The return value is
an array of profiles.


```js
service.getUserBatch({

    // use or nor your swell domain here. 
    id: ['alice@swell.company.com', 'bob@swell.company.com']
})
.then( arrayOfProfiles => {  })
.catch( error => {  });
```


### Sessions

Majority of API methods require to start an user session with the SwellRT server. 

SwellRT supports multiple sessions per browser session, it means you can have different open sessions in different tabs of the Web browser and to remember them after browser is closed.


#### login()

Log a user in the SwellRT server.

```js
    service.login({
      id : 'alice@swell.company.com',
      password : '*****'
    })
    .then( profile => {   });
```

To log in as anonymous user use an empty password:

```js
    service.login({
      id : swell.Constants.ANONYMOUS_USER_ID,
      password : ''
    })
    .then( profile => {   });
```

#### listLogin()

Lists all sessions opened in the browser. These can be resumed without providing a password again.

```js
    service.listLogin({})
    .then( userIdArray => { 

      });
```

The response is an array of user ids:

```js
  [
     'alice@swell.company.com',
     'bob@swell.company.com',
     '_anonymous_yjnizfql0tz41sxsjmg2nfh9f@local.net'
  ]
```

#### resume()

Resume an opened session that is still active in the server.
On success, returns the user's profile data. 

```js
    service.resume({
      id : 'alice@swell.company.com'
    })
    .then( profile => {  });
```

If no id parameter is passed, resume the last opened session.

```js
    service.resume({})
    .then( profile => {  });
```


#### logout()

Closes the current session with the server and dispose all connections and session data.
Optionally accept the user id of the session to be closed.

```js
    service.logout({
      id : 'bob@swell.company.com'
    })
    .then({  ...  });
```


### Collaborative Objects

SwellRT allows to store and share data in real-time using *collaborative objects*.
They can be thought as JSON documents with a special syntax and methods to access and change their properties.

A *collaborative object* has an **unique identifier on Internet**, for example 

```mycompany.com/s+T6Ad2s2TC2A```, 

where first part is the domain of the SwellRT server, and the second part is an id, provided by the client's app or randomly generated by SwellRT.

Apps using SwellRT must open objects first in order to use them. When different instances of one app have open same objects, they can share data in real-time through them.

```

     App instance #1 (Alice)
             ^
             |
             v
 -------------------------------
 |                             |
 |    Collaborative Object     |
 |                             |<-----> App instance #3 (Chris)
 | mycompany.com/s+T6Ad2s2TC2A |
 |                             |
 -------------------------------
              ^
              |
              v
     App instance #2 (Bob)


```

Changes in a collaborative object are persisted in the server and transmitted to all instances in real-time.



#### open()

Load or create a collaborative object openning a live connection with the server.

Creates a new object, set an auto generated id:

```js
  service.open({})
  .then( object => {

    console.log(object.id);

  })
  .catch( error => { ... });
```

Open or create (if it doesn't exist);


```js
  service.open({

      id : "local.net/s+T6Ad2s2TC2A"

  }).then( object => {

  })
  .catch( error => { ... });
```

Multiples calls to the *open()* method for the same object id will return the same reference to the object, keeping same connection and resources.


#### close()

Closes a collaborative object releasing connection with server and its local resources.
Any change or mutation to the object after this call will throw an exception.

```js
  service.close({

      id : "local.net/s+T6Ad2s2TC2A"

  }).then({  })
  .catch( error => { });
```




#### Object metadata and events


In this section we assume we have a collaborative object in the variable **object**.

There some general properties and methods to control object's metadata:

`object.id` 

Id of the collaborative object.

`object.getParticipants():array`

Returns an array of users that can access this object.

`object.addParticipant(userId: string)`

Shares the object with an user.

`object.removeParticipant(userId: string)`

Stop sharing the object with an user.

`object.setPublic(enable: boolean)`

Share/unshare the object with anyone.

`object.setStatusHandler(handler: function)`

Sets a handler to get notified of metadata events. 
An example handler follows:

```js

  object.setStatusHandler(function(event) {

      console.log(event.objectId);

      if (event.type == swell.Constants.OBJECT_ERROR) {
        throw event.exception;
      }

      if (event.type == swell.Constants.OBJECT_UPDATE) {
        // information about deltas transmission
        event.inflight;
        event.unacknowledge;
        event.uncommitted;
        event.lastAckVersion;
        event.allDataCommitted;
      }

      if (event.type == swell.Constants.OBJECT_PARTICIPANT_ADDED ||
          event.type == swell.Constants.OBJECT_PARTICIPANT_REMOVED) {

          console.log(event.participantId);
      }

      if (event.type == swell.Constants.OBJECT_CLOSED) {
          console.log('Object closed.');
      }

    });
```

#### Object operations

Now is time to store and share data using a collaborative object.
In next parts of this documentation we suppose we want to create a collaborative task board where different users can add, edit, remove and arrange tasks in a typical scrum's task board.

**Primitive types**

First, lets work with simple properties to define some general
information about our task board:

```js

object.put('type','task-board');
object.put('name','JetPad Development Task Board');
object.put('sprint', 3);
object.put('active', true);
object.put('author', 'Alice')

```
> *Properties like these are usually added right after creating a new object, so we define general properties meaningful for our application.*

To remove a property use the *remove* method:

```js
object.remove('author');
```

and to update a value just use the *put* method again:

```js
object.put('sprint', 4);
```

finally, retrieve values with the *get* method:


```js
object.get('sprint'); // return 4
```

**Nested properties** 

Objects can store nested properties like in a JSON document, as nested maps/objects or arrays.

**Maps**

Let's imagine we want to store a set of tasks in our board, firstly define a new property to room tasks:

```js
var tasks = object.put('tasks', swell.Map.create());
```

Now we can start adding tasks in two ways:

```js

var task = {
  name: 'Desgin Welcome Screen',
  description: '...',
  assignedTo: 'alice@mycompany.com',
  hoursEstimated: 18,
  hoursExecuted: 7
};


var tasks = object.get('tasks');
```

a) static data block

```js
tasks.put('design-welcome-screen-x54d3a', task);
```
In option *a)* task object is stored as a block, you can't make further changes in any single property of it. If you want to update part of the task object, you must to fully rewrite it.

On the other hand, the task is returned as a JSON plain object, you can access its properties with normal syntax but changes are not stored back in SwellRT:

```js
var task = tasks.get('design-welcome-screen-x54d3a');
task.hoursExecuted = 12; // this doesn't mutate the object.
task.get('hoursExecuted'); // ERROR, task is a plain JSON object without methods
```


b) separated properties

```js
tasks.put('design-welcome-screen-x54d3a', swell.Map.create(task);
```

In option *b)* all properties in the task object will be create separately in the collaborative object so they can be changed individually:

```js
var task = tasks.get('design-welcome-screen-x54d3a');
task.put('hoursExecuted', 12);
console.log(task.get('hoursExecuted'));
```
However, with this option, you get values using the *get* method. In order to have a JS view of the task object, you can use the *js()* method:

```js
var task = tasks.get('design-welcome-screen-x54d3a').js();
console.log(task.hoursExecuted));
```

Other useful methods for in a map/object property are:

```js
  var tasksMap = object.get('tasks');

  tasksMap.isEmpty();
  taskMap.has('task-x'); // true iff 'task-x' is a property
  taskMap.keys(); // return array of property names

```

*Lists*

```js
var followers = object.put('followers', swell.List.create());
```

Lists can store simple properties or nested maps and lists:

```js
followers.add('bob@mycompany.com');
followers.indexOf('bob@mycompany.com'); // 0
followers.get(0); // bob@mycompany.com
followers.isEmpty(); // false
followers.size(); // 1
followers.remove(0); 
```





**Mutation events**

When an object is opened by different collaborators (e.g. in two different browser windows, open the same public object)
any change in a data property is automatically sync and updated in each open instance. If you want to listen when changes are made, register a listener in a property:

```
	service.listen(jso.quiver, (event) => {

        // Note: this handler is invoked for local and remote changes.

        if (event.type == swellrt.Event.UPDATED_VALUE &&
            event.key == "surfboard-2-size") {

          let updatedValue = event.value.get();

        }

   });
```

A listener for a particular property can receive events in any child property. To avoid propagation of events up to the object's property tree, do return a "false" value.


### Handling connection status

TBC

**service.addConnectionHandler(`ConnectionHandler`)**

Set a listener for server connection events.

```
service.addConnectionHandler(
  function(status, error) {
    ...
  }
```

_status_ values are:

* swellrt.Service.STATUS_CONNECTED
* swellrt.Service.STATUS_DISCONNECTED
* swellrt.Service.STATUS_CONNECTING
* swellrt.Service.STATUS_ERROR

An error value means that an unrecoverable error has been detected and your app will
need to restart again all SwellRT resources again.



### Search

(Not available yet)
