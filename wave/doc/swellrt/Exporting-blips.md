

## Working with blips

First, let's play with an object and text pads/documents...

Go to Pad demo at http://localhost:9898/demo-pad.html

Create some pads using the "save..." option.

Open the browser's debug console (Chrome/Chromiun is recommended),
in this example, the global variable "object" has the SwellRT object
managing all pads in the screen.

From the debug console, the id of the object is retrived with command:

```
object.getId(); // "local.net/demo-pad-list"
```

and following command shows pad names stored in the object:

```
object.get('documents').keys();
```

Each pad (or document) is stored within the object in a blip. To get a list of all blips 
in the object run:

```
object._debug_getBlipList();
```

Blip's Ids starting with prefix "t+" stands for text blips.

To get the content (XML) of a text blip, run:

```
object._debug_getBlip("t+ ... ");
```


Now you know how texts are stored and how are identified in SwellRT/Wave.
It is time to figure out how to work with these blips in the server.

The "VersionServlet" lists versions of a blip and its content, for example 
(remember to replace the text blip id with yours)

```
http://localhost:9898/version/local.net/demo-pad-list/local.net/data+master/t+mGLABSckbPA
```

The URL is formed by...

"local.net/demo-pad-list" -> Object Id = Wave Id
"local.net/data+master" -> Object Container Id = Wavelet Id
"t+mGLABSckbPA" -> Blip id

and the return value is a list of blip's versions, let's take the content of the blip for one version...
let's suppose we got the following last line in the previous call:

{"version":32639,"hash":"Zy0RCu2lyN6XoRkcrxVY58eXy4U=","author":"_anonymous_1gvoqplzcfeogoc3sv546hchi@local.net","time":"2017-05-20T17:16:43.606+02:00[Europe/Madrid]"}


Then get the content of the blip for the version 32639:Zy0RCu2lyN6XoRkcrxVY58eXy4U with this call...
```
http://localhost:9898/version/local.net/demo-pad-list/local.net/data+master/t+mGLABSckbPA/32639:Zy0RCu2lyN6XoRkcrxVY58eXy4U
```

Cool, now you can see the XML content of the blip.

But we want to implement a servlet to export a blip's XML to PDF. So we can use the "VersionServlet.java" as starting point. 
In line 424, the blip data is retrieved, and we can access the XML interface of the blip with 

```
blip.getContent().getMutableDocument()
```

"getMutableDocument()" returns a ReadableDocument interface to work with XML as a DOM API. Some helper functions to work with a XML Document can be found in class DocHelper.

We have to read the XML and generates the PDF, first iteration of this task can be just to export text and return it in the servlet.


