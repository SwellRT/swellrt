

# Editor

SwellRT Web API provides a customizable rich-text editor
that allows real-time editing of SwellRT text nodes.

The text editor experience can be extended and enhanced by developers using the following features available in the API:

- Custom handlers for **carets** and **text selections**.
- Custom and Predefined **text annotations**.



## Text nodes

### Creation

A local text node is create with the following sentence:

```js
var text = swell.Text.create("Some initial text");
```

To really have a collaborative text, share the node as 
part of a collaborative object:

```js
service.open({ id : "shared-text-object" })
    .then(object => {
		
		if (!object.node('text')) {
			object.set('text', text);
		}

		// remind to get this reference!
		text = object.get('text');

	});
```


### Rendering

Text nodes can be directly rendered into a `div` element of the
DOM tree. Rendered text can't be edited by users unless the text node is wrapped by an editor instance. However, a rendered view of a text is live, showing, in realtime,  changes performed remotely or locally via the API.

### Text node functions 

**text.attachToDOM(element)**

Attach the text node to an existing DOM element and render it.

**deattachFromDOM()**

Deattach this text node from DOM.

**isAttachedToDOM():boolean**

Check whether the node is attached or not.


## Editor

### Basic functions

The SwellRT editor component, allows users to edit a text node
in a Web page. It provides some features by default: *key events, clipboard copy/paste, embedded widgets, diff/history views, etc.*

**swell.Editor.create([divElement])**

Create a new editor instance. If a `div` element is provided, text nodes assigned to the editor will be rendered inside that element. Otherwise, the text node is required to be attached previously to a DOM element.

```js
var editor = swell.Editor.create(document.getElementById("editor"));
```

**editor.set(textNode)**

Assign a text node to the editor, rendering the text inmediately.

**editor.clean()**

Unset the text node previously assigned to this editor. Stop rendering the text inmediately.

**editor.edit(boolean)**

Allow to user to edit interactively or not the text.

**editor.isEditing()**

Checks whether the editor is in edit mode.

**editor.hasDocument()** 

Checks whether the editor has attached a text node.

### Caret and Selections

Selections are those contiguous parts of a text selected by an user when she interacts with the editor canvas. Selections also
points to the location of the editor's caret: the caret is always at the beginning of a selection, hence, a zero length selection stands for the caret.

The Editor's API allows developers to listen when selections
change using registering a selection handler in the editor:

```js
editor.setSelectionHandler((range, editor, selection) => {
    

      });
```

The selection handler receives following arguments:

**editor**, a reference to the editor's object.

**range**, the selection range. See *selection.range*. 

**selection**, bundled information about the current selection. It can be also retrieved programatically:

```js
var selection = editor.getSelection();
```

A selection object has following properties and methods:

*selection.anchorNode*, DOM node in which the selection begins.

*selection.anchorOffset*, number representing the offset of the selection's anchor within the anchorNode. If anchorNode is a text node, this is the number of characters within anchorNode preceding the anchor. Zero otherwise.

*selection.focusNode*, DOM node in which the selection ends.

*selection.focusOffset*, number representing the offset of the selection's anchor within the focusNode. If focusNode is a text node, this is the number of characters within focusNode preceding the focus. Zero otherwise.


*selection.range*, start and end indexes of the selection within the text. For example:

```js
 { 
	 start: 11,
	 end: 25
 }
```

*selection.isCollpased*, boolean value, true iff range has same start and end positions, that is, if is a caret,

*selection.getFocusBound()*, gets the y-bounds of the cursor position.


*selection.getSelectionPosition()*, coordinates of the current selection start relative to a parent element, for example:

```js
{
	left: 798,
	top: 274,
	offsetParent: <DOM element>
}
```

*selection.getAnchorPosition()*, coordinates of the current selection end relative to a parent element.


## Annotations

An annotation is a set of tree values: `(key, value, range)`:

**Annotation key**: type of the annotation, it is just a string
that allows to group annotations by its value. Examples of annotation types in a text are "comments", "hyperlinks", "bookmarks". 

**Annotation value**: the value of the annotation, for example a "URL" for an hyperlink annotation.

**Annotation range**: a contiguous part of the text where the annotation applies to. Consider the following text (first char is always at position 0):

```
	It was one of those March days when...
	|                   |   |					  
	0                  20   24 	  
```

In this example, we could set an annotation for the word "March" with the following data:

- *range: (20,24)*
- *name/key: month*
- *value: 4*

Value and key fields are strings and the range is an ordered pair of positions inside the text.

Annotations are stored as part of a text node and like the
rest of SwellRT concepts, can be handled in real-time.
However, how annotations are rendered is a responsability of the text/editor and its extensions.


### Predefined annotations for text styling.

The SwellRT editor provides a predefined set of annotation types that controls the rendered appearence and style of the text using the CSS syntaxis. 

Use as follows to set a H1 header in the current selection.

```js
editor.setAnnotation("header", "h1");
```

For additional ways of setting annotations jump to the *Annotations Functions* section:


#### Paragraph style annotations

| Name | Values |
|----- | ------ |
|header| *h1...h5* |
|textAlign| *left, center, right, justify* |
|list     | *decimal, unordered* |
|indent	  | *outdent, indent* |

#### Text style annotations

| Name | Values |
|----- | ------ |
|backgroundColor| *a CSS color hex #code* |
|color			| *a CSS color hex #code* |
|fontFamily		| *a CSS font value* |
|fontSize		| *a CSS font size* |
|fontStyle		| *a CSS font style* |
|fontWeight		| *a CSS font weight* |
|textDecoration | *a CSS text decoration value* |
|verticalAlign  | *a CSS vertical align value* |

The values of these annotations will be added as inline CSS styles of the annotated text.

### Custom annotations

For custom annotations, developers can provide custom render logic within the editor canvas.

First of all, a custom annotation type (key) must be registered in the API with the following syntax:

(**NOTE: annotations must be registerd before creating any Editor instance.**)


```js
swell.Editor.AnnotationRegistry.define(
	<key>, 
	<css-class>, 
	{ 
		<inline-css-rule>,
		<inline-css-rule>,
		...
	});
```

- `<key>` (string) the annotation type aka the annotation key.
- `<css-class>` (string) a CSS class name.
- `<inline-css-rule>` a CSS inline rule.


These arguments customize how the annotated text will be rendered in the DOM.

Having the following annotation definition for "draft":

```js
swell.Editor.AnnotationRegistry.define(
	'draft', 
	'draft', 
	{ 
		'background-color': 'grey'
	});
```
a text annotated with it will be rendered as follows:

```html
	<span 
		class="draft" 
		style="background-color: grey">
		bla bla bla
	</span>

```



### Annotations functions


**editor.setAnnotation(key, value, [range])**

Annotates with the provided key and value, the text in the current selection or the one selected by the provided range.

If a *null* value is set, the annotation is removed only in given range, spliting the rest of the annotation.


**editor.clearAnnotation(keys[], [range])**

Remove all annotations with key within the array *keys* that are partially or totally contained in the range.

When no range is provided, current selection is assumed.

**editor.getAnnotations(keys[], [range])**

Get all annotation for the given keys. 
If *range* is not provided, current selection range is used.

**editor.getAnnotationsWithValue(keys, value, [range])**

Similar to previous function but filtering out annotations with the given value.

*Annotation values*

Annotations obtained from *getAnnotationsXXX()* methods returns an object with one array for each annotation key found, for example:

```js
{
	link: [ ... ],
	draft: [ ... ],
	style/fontSize: [ {

		key: 'style/fontSize',
		value: '20px',
		line: <DOM element>,
		node: <DOM node>,
		range: {
			start: 10,
			end: 21
		},
		text: 'hello world',
		searchMatch: 0


	} ]

}
```

Each element on these arrays have self-explanatory properties.
In particular the *searchMatch* property represents how the the annotation overlaps the searched range:

- *swell.Annotation.MATCH_IN* the search range is equals or inside the annotations's range.
- *swell.Annotation.MATCH_OUT* the search range is partially out of the annotation's range or spans beyond it.



*Ranges*

Usually you won't need to provide a hand written range (because you will get one from another API function) but if necessary, a range is a property with following properties:

```js
var range = {
	start: 11,
	end: 23
};
```

In situations when you need to indicate a range spaning the whole document use the constant:

```js
var fullDocumentRange = swell.Range.ALL;
```



### Anotations Overlap

Overlapped annotations with same key can be problematic in some scenarios. Let's suppose the following example of an annotated text representing a comment (with value A):

```
              	  {comment,A}  
                  |         |
	It was one of those March days when...
	              					  
	                	  
```

If we want to comment another part of the text that overlaps first comment's text, by default, the editor will split and override the former annotation with the new one (B):

```
              	  {comment,A}  
                  |    |
    It was one of those March days when...
	                    |        |              					  
	                    {comment,B}
```

In this case, we have lost the reference of comment A in the word "March". 

To avoid this situation you can use special methods:

**editor.setAnnotationOverlap(name, value, [range])**

Following the previous example, this method will generate following annotations:

```
                {comment,A}  {comment,B}  
                  |    |      |  |
    It was one of those March days when...
	                    |    |  
	                  {comment, A,B}
```

Now, the word "March" will be annotated with annotation "comment" with a combined value "A,B".

In order to revert this annotation properly, use the method:

**editor.clearAnnotationOverlap(name, value, [range])**


### Annotation events

Annotation events are triggered both when the annotation is created or deleted in the document and when it is rendered or unredered in the DOM.

 An Annotation handler must be registed right after the annotation is defined, for example:

```js
swell.Editor.AnnotationRegistry.define("mark","mark");

swell.Editor.AnnotationRegistrysetHandler("mark", (event) => {

      console.log("annotation event ["+event.type+"] "+event.annotation.key+"="+event.annotation.value+", "+event.annotation.range.start+":"+event.annotation.range.end);

    });	
```
The event object is passed to the handler anytime the given annotation throws an event. This object has following properties:

```js
event = {

	type: <int>,
	newValue: <string>,
	annotation: <annotation value>
	domEvent: <dom event>

}
```

There are two type of events, DOM-level and Text-level. 

*DOM-level events* are triggered anytime the editor needs to repaint annotations:

- *swell.AnnotationEvent.EVENT_DOM_ADDED* 
- *swell.AnnotationEvent.EVENT_DOM_MUTATED*
- *swell.AnnotationEvent.EVENT_DOM_REMOVED*
- *swell.AnnotationEvent.EVENT_DOM_EVENT*

*Text-level events* are only triggered when annotations are created or removed in the text:

- *swell.AnnotationEvent.EVENT_CREATED*
- *swell.AnnotationEvent.EVENT_REMOVED*


### Custom carets

The text editor renders cursors for all user editing a text concurrently. Developers can customize the displayed caret configuring the editor with a caret factory.

A custom caret is a simple Javascript object that must provide following properties and methods:

```js
var caret = new Caret(); // our custom caret type
caret.element; // DOM element of the rendered caret.
caret.update(caretInfo); //called to refresh the caret element.
```

Configure a caret factory of using the editor global property "caretFactory":

```js
swell.Editor.configure({
	caretFactory: function() { return new Caret(); }
});
```

An editor instance will call `update(<caretInfo>)` method anytime it suposes the caret needs to be rendered again.

A caret info object contains following information that can be used to customize the caret DOM:

```
	caretInfo = {
		timestamp: <long>, // last time the caret position changed 
		session: <Session>, // the session object
		position: <int> // the caret position in the text
	}
```

A session object provides following information of a user's session:

```
	session = {
		sessionId: '<session-id>',
		id: '<user id>`,
		name: 'user full name',
		nickname: 'user nick name',
		color: {
				cssColor: 'rgb(RR,GG,BB)',
				hexColor: '#RRGGBB'
				},
		lastAccessTime: <long>,
		firstAccessTime: <long>
	}
```


### Advanced configuration

Editor instances can be tunned with some adavanced
settings that can be set using the following method:

```js

swell.Editor.configure({
	consoleLog: true
});
```

A list of available properties that can be passed to the *configure* method follows:

| Setting name | Value |
|----- | ------ |
|traceUserAgent| (boolean) shows the browser's UA string in console |
|logPanel| (Element) DOM element where editor's log messages are printed |
|consoleLog| (boolean) print log messages in browser's console |
|undo     | (boolean) enable/disable editor undo |
|caretFactory	  | (CaretViewFactory) a factory of caret objects |

For more information on Editor internals check out
*SEditorStatics* class.

## Development tips

### Logging

Enable console log:

```js
swell.Editor.configure({
	consoleLog: true
});
```

Enable HTML log panel:

```js
swell.Editor.configure({
	logPanel: document.getElementById("log"),
});
```

Logging is available when GWT log level is "DEBUG" or "ERROR". Check out current log level in "build.gradle" and GWT module files "ServiceFrontendDev.gwt.xml"


### Wave Harness Editor 

The Wave project (where Swell is on top) has a GWT module to play with an standalone editor instance.

```sh
./gradlew editorHarness
```

```
http://localhost:9876/org.waveprotocol.wave.client.editor.harness.EditorTest/EditorTest.html
```

- Activate GWT SuperDev mode if necessary with the provided links

Configure extension of the editor in `DefaultTestHarness.java`

Harness editor runs solely in client side, hence no running server is required. 


### Swell Editor Sandbox

Editor sandbox is a small GWT app to play editor features. Run with following commands:

```sh
./gradlew sandbox
```

```
http://127.0.1.1:9876/org.swellrt.sandbox.editor.Sandbox/editor.html
```

Sandbox editor runs solely in client side, 
no running server is required. 

### Developer's recipes

*Running GWT compiler and DevMode from cmd line*

Look for the `gwt-dev.jar` in local maven repo `find ~/.gradle/caches/ | grep gwt | grep .jar`

`java -cp gwt-dev.jar com.google.gwt.dev.Compiler`

