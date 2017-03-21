For installation instructions please read  [README.md](https://github.com/P2Pvalue/swellrt/blob/master/README.md)

At this moment, SwellRT only provides a JavaScript client for Web projects.

Examples of this section can be found in the small demos provided in default installations of SwellRT: [basic form demo](https://github.com/P2Pvalue/swellrt/blob/master/wave/webapp/demo-form.html) and [text pad demo](https://github.com/P2Pvalue/swellrt/blob/master/wave/webapp/demo-pad.html)

### Basic Concepts

The aim of SwellRT API is to provide a easy programming model for real-time collaboration. It is based in two abstractions:


**Collaborators**

Users registered in any SwellRT server. Also there is a special _anonymous_ collaborator.
Each collaborator belongs to a server instance, and their ID has the syntax _(user name)@(server name)_


**Collaborative Objects**

A collaborative object is a data structure shared by one or more _collaborators_.
Each collaborator can have different level of access to an object and its parts.

An object, when it is used by a collaborator has three different data zones:

- Persisted zone: any change in this zone is automatically sync and persisted among collaborators.
- Private zone: only the current collaborator has access to this zone. This data is persisted.

Each zone is a nested structured of arrays, maps and primitive values.


### Basic steps

After installing a local instance of SwellRT, make your Web project to load the SwellRT client:

```
<script src='http://localhost:9898/swellrt-beta.js'></script>
```

Get a reference of the API instance (we use the names "service" or "s") registering a callback to be called after the client is loeded and ready.

```
<script>

  swellrt.onReady( (service) => {
    window.service = service;
   });

</script>    
```

The _service_ instance passed to this callback uses Promises as return values. An equivalent way to get this promise-based service instance is:

```
<script>

  swellrt.onReady( () => {
    window.service = swellrt.runtime.get();
   });

</script>  
```

Get a service instance providing callback-based syntax calling

```
<script>

  swellrt.onReady( () => {
    window.service = swellrt.runtime.getCallbackable();
   });

</script>  
```


**Login**

To work with objects a login is required:

```
    service.login({
      id : swellrt.Service.ANONYMOUS_USER_ID,
      password : ""
    })
    .then(profile => {  ...  });
```

Anonymous users are associated with the current browser session.

**Creating and getting objects**

_open()_ will load an object or create it, if it doesn't exist.
Leave _id_ field empty to create an object with an auto generated id.


```
	service.open({

	    id : "my-first-collaborative-object"

	})
	.then( (result) => {

		let obj = result.controller;
		obj.setPublic(true);

	})
	.catch( (exception) => {

	});
```

_obj_ is the view of the collaborative object for the logged in collaborator. It is the persisted zone.
For this example, lets set the object as _public_ in order to be accessible by other anonymous collaborators.

To get the personal zone use:

```
	let privObj = obj.getPrivateArea();
```


**Working with collaborative objects**


Using objects with map syntax

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
	// Get a javascript proxy
	jso = obj.asNative();

	// Reading properties
	jso.address.street;

	// Adding properties
	jso.address.state = "Hawaii";


	// Adding nested map - as mutable properties

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

        if (event.key == "surfboard-2-size" && event.type == swellrt.Event.UPDATED_VALUE) {
          let updatedValue = event.value.get();
        }

   });
```

A listener for a particular property can receive events in any child property. To avoid propagation of events up to the object's property tree, do return a "false" value.
