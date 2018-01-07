This documents is a working draft of Swell's access control system design.

## Wave

We'd like to have an access control system that fits in current wavelet system (collaborative objects in Swell terminology).

Now, access control is based on a list of participants in each
wavelet. Anyone in the participant list has full control over
the wavelet, it means following operations:

- read any property of the object
- write any propery of the object
- add or remove participants

## Use cases


For shake of clarity, let's take jetpad text editor's use cases:

- a) Allow documents to be fully accesible by anyone
- b) Allow documents to be read publicly but only edited by some users.
- c) Allow documents to be commented or get suggestions for some users, but they can't change text.
- d) Allow document participants be managed only by some specific "owners" or "administrators"
- e) Allow groups, any user whithin a group has the access granted to the group.
- f) Allow anonymous users.

## Access control

### Types of participants

* Regular `alice@swell-provider.net`
* Group `$devops@swell-provider.net`
* Meta participant wildcard for any regular participant or group: `~@swell-provider.net`
* Meta participant wildcard for any anonymous participant: `%@swell-provider.net`
* Meta participant wildcard for any participant (regular or anonymous): `@swell-provider.net`. 

### Supported Operations


* Read only as particular role (e.g. only read text, but not comments)
* Read
* Write only as particular role (e.g. only write suggestsions or comments)
* Write
* Admin participants, allows to add, remove or change access previleges of other participants.

Each operation, includes previous one.

### Access Control list

Waves/Collaborative objects has a list of participants for whom access is granted.


In addition to original list of participants for an object

```js
object.participants = [ 
    'alice@swell-service.net',  'bob@swell-service.net']
```

the syntax is extended to support specific operation privileges:

```js
object.participants = [ 
    'alice@swell-service.net',  
    'bob[r]@swell-service.net',
    'caroline[w]@swell-service.net']
```

Alice has full control, Bob only can read and Caroline can read and write.

Define specific roles, they should be forced by the client app

```js
object.participants = [ 
    'alice@swell-service.net',  
    'bob[w:suggest:comment]@swell-service.net',
    'caroline[r:text]@swell-service.net']
```

Also, same syntax applies for psuedo users:

```js
object.participants = [ 
    'alice@swell-service.net',  
    'bob[w:suggest:comment]@swell-service.net',
    '[r:text]@swell-service.net']
```

Any registerd user can read with role *text*.

```js
object.participants = [ 
    'alice@swell-service.net',  
    'bob[w:suggest:comment]@swell-service.net',
    '%[r:text]@swell-service.net']
```
Anonymous users can also read with role *text*.


```js
object.participants = [ 
    'alice@swell-service.net',  
    'bob[w:suggest:comment]@swell-service.net',
    '%[r,w:suggest:comment]@swell-service.net']
```
bob[w:a:suggest]