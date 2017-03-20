
_Note: SwellRT is migrating to beta version. All previous alpha API methods will be migrated eventually._

* [Service Runtime](#service-runtime)
* [Users](#users)
* [Sessions](#sessions)
* [Collaborative Objects](#collaborative-objects)
* [Search](#search)

**Promises and callbacks**

The Web API is provided in callback and promise syntaxes (See next section). API methods are analogue in both cases. For callback based methods, use as final argument a callback object. For example, this is the callback based _login()_ method:

```
    serviceCallback.login({
      id : swellrt.Service.ANONYMOUS_USER_ID,
      password : ""
    },

    {
        onSuccess: function(profile) {
          ...
        },

        onError: function(error) {
          ...
        }
    });
```
Its analogue promisable version is:

```
    servicePromise.login({
      id : swellrt.Service.ANONYMOUS_USER_ID,
      password : ""
    })
    .then( profile => { ... })
    .catch( error => { ... });
```


### Service Runtime


**swellrt.runtime.get()**

Returns a SwellRT service instance with promise-based syntax.
Use this function inside the _swellrt.onReady()_ callback.

**swellrt.runtime.getCallbackable()**

Returns a SwellRT service instance with callback-based syntax.
Use this function inside the _swellrt.onReady()_ callback.

**swellrt.onReady(`callback`)**

Register a callback to detect when the SwellRT service runtime is ready to be used.

The _service_ instance passed to this callback uses the promise-based syntax.

```
<script>

  swellrt.onReady( (service) => {
    window.service = service;
   });

</script>    
```

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

### Users

#### User IDs

SwellRT users are identified by a email-like string where the domain part is the domain of the SwellRT server for that user, for example:

_tom@swellrt.company.com_

To ease the use of the API, all methods expecting an user ID will admit just the _name_ part of the ID as valid input, assuming she belongs to the current SwellRT domain. Consider using full IDs in federated installations of SwellRT.  

#### Anonymous users

SwellRT supports anonymous users. They don't have profile and are associated to a browser session. The special ID for anonymous users is
stored in the constant:

```
swellrt.Service.ANONYMOUS_USER_ID
```

**createUser(`UserProfile`)**

Create a new user with the provided profile information.

```
service.createUser({

    id: "tom",
    password: "xxxx",
    email: "tom@company.com", // (Optional)
    locale: "en_EN", // (Optional)
    avatarData: // (Optional) Base64 encoded image   

}) .then( userProfile => { ... })
.catch( error => { ... });
```

**editUser(`UserId`,`UserProfile`)** (Not available yet)

**getUser(`UserId`)** (Not available yet)


### Sessions

**login(`UserCrendentials`)**

```
    service.login({
      id : "tom",
      password : "xxxx"
    })
    .then(profile => {  ...  });
```

For anonymous users use a empty password string.

**resume(`[UserId]`)**

Resume the last browser session if it is still active in the server.
On success, returns the user's profile data and the session token.
Optionally accept the user ID of the session to be resumed.

```
    service.resume({
      id : "tom"
    })
    .then( profile => {  ...  });
```

**logout(`[UserId]`)**

Closes the current user session with the server and dispose all connections and session data.
Optionally accept the user ID of the session to be closed.

```
    service.logout({
      id : "tom"
    })
    .then({  ...  });
```

### Collaborative Objects

#### Object IDs

Object IDs are strings with syntax _(swellrt domain)/(object id)_.

**open(`ObjectId`)**

Load a collaborative object and open a live connection with the server. Creates the object if no ID is provided or it doesn't exist previously in the server. SwellRT will assign an auto generated ID when no one is provided.
Repeated calls to _open()_ will return always the same object's instance, no duplicating connections with the server.

```
  service.open({

      id : "local.net/s+T6Ad2s2TC2A"

  }).then( response => {

      var controller = response.controller;
      var object = response.object;

  })
  .catch( error => { ... });
```

The _response_ object exposesthe collaborative object in two forms: the _object_ property is a reference to a JavaScript's proxy of the object. It allows to work with the collaborative object as a regular JavaScript object.

The _controller_ property is a reference to the collaborative object's full API.


**close(`ObjectId`)**

Closes the collaborative object releasing connection with server and any local resource.
Any write (mutation) operation called on any reference of this object will throw an exception.

```
  service.close({

      id : "local.net/s+T6Ad2s2TC2A"

  }).then( response => { ... })
  .catch( error => { ... });
```

#### Object Controller

**controller.getId()**

**controller.addParticipant(`Participant`)**

**controller.removeParticipant(`Participant`)**

**controller.setPublic(`isPublic: boolean`)**

**controller.getPrivateArea()**

**controller.asNative()**

**controller.setStatusHandler(`StatusHandler`)**

```
  controller.setStatusHandler(function(event) {

      event.objectId; // the object ID

      if (event.type == swellrt.StatusEvent.ERROR) {
        event.exception;
      }

      if (event.type == swellrt.StatusEvent.UPDATE) {
        // operations live status
        event.inflight;
        event.unacknowledge;
        event.uncommitted;
        event.lastAckVersion;
        event.allDataCommitted;
      }

      if (event.type == swellrt.StatusEvent.PARTICIPANT_ADDED ||
        event.type == swellrt.StatusEvent.PARTICIPANT_REMOVED) {

          event.participantId; // the participant ID
      }

      if (event.type == swellrt.StatusEvent.CLOSED) {
        // this object can't be used anymore
      }

    });
```

#### Working with object data

The controller object works as a map, where new maps or simple value properties can be nested:

```
	// primitive values
	controller.put("name", "Kelly Slater");
	controller.put("job", "Pro Surfer");
	controller.put("age", 42);

	// add nested map
	controller.put("address", swellrt.Map.create().put("street", "North Coast Avenue").put("zip", 12345).put("city","Honololu"));

	// get root level keys (properties)
	controller.keys();

	// access values
	controller.get("address").get("street");

```

Get a Javascript proxy object calling to _asNative()_:

```
	jso = controller.asNative();

	// Reading properties
	jso.address.street;

	// Adding properties
	jso.address.state = "Hawaii";


	// Adding nested a map
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


### Search

(Not available yet)
