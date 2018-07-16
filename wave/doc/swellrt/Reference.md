
This section describes all methods in the Web API of Swell.

* [Web API](#web-api)
* [Users](#users)
* [Sessions](#sessions)
* [Collaborative Objects](#collaborative-objects)
* [Handling connection status](#handling-connection-status)
* [Search](#search)


### Web API


From your project's html file, import the `swell.js` script from a Swell server: 

```html
<script src="http://localhost:9898/swell.js"></script>
```

Then, register a callback to get notified when the script was fully loaded so you can start using the API exposed in the *service* object:

```js
  swell.onReady( (service) => {
   
    // service = Swell's API entry point

   });
```

**API's entry point**

The *swell* global variable is set in the *window* object by the *swell.js* script. However it must be thought as a namespace for the Swell library and not as an entry poiny for the API.

The actual object containing the methods of the API is the one passed to the *onReady()*'s callback, the *service* variable in the example. Keep this reference in your code for further use of the API.

The rest of this documentation use the name *service* to refer to the API's entry point.

You also can get a reference to the entry point calling the global method:

```js
  var service = swell.runtime.get();
``` 


**Promises and Callbacks**

By default, API's methods provide a **promise-based** syntax:

```js
    service.login({})
    .then( user => {  })
    .catch( error => {  });
```


In case you rather work with callbacks you can get the callback-based entry point of the API with this global method:

```js
  var callbackBasedService = swell.runtime.getCallbackable();
```

All API methods follow the same syntax for callbacks, for example:

```js
    callbackBasedService.login({},
    {  
       onSuccess: function(user) {  },
       onError: function(error) {  }  
    });
```



### Users

Swell users are identified by a email-like strings where the domain part is the domain of a Swell server, for example:

*alice@swell.company.com*

All methods expecting an user id will admit just the *name* part (before the @), assuming she belongs to the current Swell domain. 

In federated installations of Swell, is recommended to use full id syntax to avoid issues.




**createUser()**

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


**editUser()**

Edits the profile of the user currently logged in.
To delete the value of an attribute, add it to the request with an empty value.

```js
service.editUser({
    name: 'Tomas Major '   // change name
})
.then( profile => {  })
.catch( error => {  });
```

**getUser()**

Retrieves profile information of one single user.


```js
service.getUser({

    id: 'tom@swell.company.com', // use or nor your swell domain here.               
})
.then( profile => {  })
.catch( error => {  });
```


**getUserBatch()**

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

**password()**

Changes user's password having an old password or a temporary recovery token:

```js
service.password({

    id: 'alice',
    oldPassword: '******',
    newPassword: '******'
})
.then( result => {  })
.catch( error => {  });
```


```js
service.password({

      id: 'alice',
    token: 'AX045KS934SLAN323',
    newPassword: '******'
})
.then( result => {  })
.catch( error => {  });
```

The recovery password is obteined calling to *recoverPassword()* 

**recoverPassword()**

Sends an email to the given user with a link containing password recovery token.

The user account can be identified with its email address or user id:

```js
service.recoverPassword({

    email: 'alice@mail.com',
    url: 'http://myapp/rememberpassword/$token/$user-id'

})
.then( result => {  })
.catch( error => {  });
```

or


```js
service.recoverPassword({

    id: 'alice',
    url: 'http://myapp/rememberpassword/$token/$user-id'

})
.then( result => {  })
.catch( error => {  });
```

The server will replace variables *$token* and *$user-id* with the actual values. 

Your app shoud recognize and accept that URL, retrieve user's id and password token in order to call to *password()* method.


### Sessions

Majority of API methods require to start an user session with the Swell server. 

Swell supports multiple sessions per browser session, it means you can have different open sessions in different tabs of the Web browser and to remember them after browser is closed.


**login()**

Log a user in the Swell server.

```js
    service.login({
      id : 'alice@swell.company.com',
      password : '*****'
    })
    .then( profile => {   });
```

To log in as anonymous user, don't provide any argument:

```js
    service.login({
    })
    .then( profile => {   });
```

**listLogin()**

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

**resume()**

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


**logout()**

Closes the current session with the server and dispose all connections and session data.
Optionally accept the user id of the session to be closed.

```js
    service.logout({
      id : 'bob@swell.company.com'
    })
    .then({  ...  });
```


### Collaborative Objects

Swell allows to store and share data in real-time using *collaborative objects*.
They can be thought as JSON documents with a special syntax and methods to access and change their properties.

A *collaborative object* has an **unique identifier on Internet**, for example 

```mycompany.com/s+T6Ad2s2TC2A```, 

where first part is the domain of the Swell server, and the second part is an id, provided by the client's app or randomly generated by Swell.

Apps using Swell must open objects first in order to use them. When different instances of one app have open same objects, they can share data in real-time through them.

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



**open()**

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

Object will be created with an autogenerated id,
if no id argument is provided. Optionally, a "prefix" can be passed to use in the autogenerated id.

Multiples calls to *open()* for the same object will return the same single reference.



**close()**

Closes a collaborative object releasing connection with server and its local resources.
Any change or mutation to the object after this call will throw an exception.

```js
  service.close({

      id : "local.net/s+T6Ad2s2TC2A"

  }).then({  })
  .catch( error => { });
```




#### Object metadata and live status


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
A collaborative object is a kind of special JS object with special syntax to handle real-time sync transparently. 

> In following examples, we use the var *object* as the collaborative object returned by the *open()* method.


**get()**

Returns a Javascript view of the whole collaborative object.

**get(path)**

Returns a Javascript view of part of the collaborative object, according to the provided path.

```js
object.get('tasks');
``` 

**set(path, value)**

Sets or update a property or subpart of the object referenced by a *path*.

For example, this code adds a new sub object in a property named "20170501-01" of the property "tasks":

```js
object.set('tasks.20170501-01', 
  { 
    name: 'A new task',
    description: 'bla bla bla',
    estimated_hours: 150
   });
```

With this syntax, the inserted object will we stored internally as a single piece of data, so you won't be able to listen to specific changes in a particular property, for example, "estimated_hours".
It you want to declare sub objects where any property can be listened, you must use a different syntax to make explicit this behaviour:

```js
object.set('tasks.20170501-01', 
  swell.Map.create(
  { 
    name: 'A new task',
    description: 'bla bla bla',
    estimated_hours: 150   
   }));
```

**delete(path)**

Deletes a propery of the collaborative object. For example:

```js
object.delete('tasks.new.20170501-01');
```

**contains(path, propertyName)**

Checks if a property is direct child of the path.


**Primitive data types**

Collaborative objects supports all basic Javascript basic data types:

```js
object.set('type','task-board');
object.set('name','JetPad Development Task Board');
object.set('current_sprint', 3);
object.set('active', true);
```

**Arrays / Lists**

Collaborative objects support Javascript-like arrays named lists.

Use the set method to create new lists:

```js
object.set('completed_tasks', swell.List.create()); // empty list

object.set('pending_tasks', 
  swell.List.create(['20170501-01', '20170501-02'])); // initial values
```

You can access a list element using a path expression:

```js
object.get('pending_tasks.0'); // returns '20170501-01'
```

this method is more efficient than getting the whole list as a Javascript array and then accesing index 0:


```js
object.get('pending_tasks')[0]; // returns '20170501-01'
```

Other list methods are:

**push(path, value)**  /  **add(value)**  / **addAt(value, index)**   

Adds a new value at the end of the list or in the specific index.


```js
object.push('pending_tasks','20170610-02');

object.node('pending_tasks').add('20170610-02');

object.node('pending_tasks').addAt('20170610-02', 2);
```

**pop(path)**

Returns and delete the last element of the list.

```js
object.pop('pending_tasks');
```

 **pick(index)**
 
Returns the element at the *index* position.

```js
object.node('pending_tasks').pick(2);
```



**delete(path)**  / **remove(index)**

Deletes a element in the list:

```js
object.delete('pending_tasks.1');

object.node('pending_tasks').remove(1);
```

**length(path)** / **size()**

Returns the length of a list:


```js
object.length('pending_tasks'); // returns 1;

object.node('pending_tasks').size();
```


#### Object nodes and events

Internally, each object's property can be also managed accessing to its "node" object. Use method *node()* to retrieve it:

**node(path)**

Return the node object for the property specified in the path.

 ```js
object.node('pending_tasks'); // returns a "node" object
```

Each node has its own *node()* method, so you can access child nodes:


 ```js
object.node('pending_tasks').node('20170501-01');
```

The main purpose of node objects is to declare listeners for properties in order to react to updates (local or remote):

**node.addListener(listenerFunction, [path])**

Adds a listener in the node or in a nested node according to the provided path.

```js
object.node('pending_tasks').addListener(
  function(event) {

    event.type; // int code.
    event.target; // the container node.
    event.key; // index or property name updated or removed.
    event.node; // the updated or removed node.

    var propagetEvent = false;
    return propagetEvent;
  });
```

Possible event types are:

```js
swell.Event.ADDED_VALUE;
swell.Event.REMOVED_VALUE;
swell.Event.UPDATED_VALUE;
```

A listener must return a boolean value indicating whether to propagate 
the event to listeners in ancestor nodes. 

**node.removeListener(listenerFunction, [path])**

Removes a listener previously added.


#### Friendly names for collaborative objects

Swell provide some utility methods to assign friendly names to collaborative objects:

Assign a name to an object:

```js
service.setObjectName(
    { 
      id : 'local.net/s+a5ce167256', 
      name : 'The third document' 
    })
    .then(  _names => { ... })    
```

the result **_names** is an object with all existing names assigned to the object:

```json
  { "waveId":
      
      { 
        "domain": "local.net",
        "id": "s+a5ce167256"
      },
    
    "names":[
        {
          "name": "The third document",
          "created": 1507887516893
        }
      ]
  }
```

Query all names of an object by its id:

```js
service.getObjectNames({ id: 'local.net/s+a5ce167256' })    .then( _names => { ... });
```

or to get all synonymous: 


```js
service.getObjectNames({ name: 'The third document' })    .then( _names => { ... });
```

Finally, a name can be removed from an id:

```js
service.deleteObjectName(
  { 
    id : 'local.net/s+a5ce167256', 
    name : 'The third document' 
  }).then( _names => { ... });
```

### Handling connection status

**service.addConnectionHandler(`ConnectionHandler`)**

Set a listener for server connection events.

```js
service.addConnectionHandler(
  function(status, error) {
    ...
  }
```

*status* can take following values:

```js
swell.Constants.STATUS_CONNECTED
swell.Constants.STATUS_DISCONNECTED
swell.Constants.STATUS_CONNECTING
swell.Constants.STATUS_ERROR
```

An error value means that an unrecoverable error has been detected and your app will need to restart again all Swell resources again.



### Search

(Not available yet)


### Text documents and Editor

See [editor documentation](https://github.com/P2Pvalue/swellrt/blob/master/wave/doc/swellrt/Editor.md)