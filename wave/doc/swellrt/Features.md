

### Transient storage

Collaborative objects to have a transient storage area, accesible in same way the normal area.
Data put here will be erased eventually by the server, so clients must expect it is not available always.

### Live carets 

Render carets for online remote participants editing the same text.
Editor clients can provide its own Javascript code to render participant's carets within the text.

### Live Presence

To know when remote participants of an object get online or offline.
Provide profile information of these participants (name, avatar...).
Allow to know participants' profiles.
Store presence status in transient storage of the collaborative object.

### Swell Json API

Provide a Json API that could be used in both GWT and pure Java platforms. Internally it could rely on
both GWT Json utils and Gson library. Allow Swell's Json data type (SPrimitive) to work with this it seamsly.

### Document History & Diff display  