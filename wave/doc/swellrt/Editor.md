

Swell Web API includes a customizable rich-text editor.

## Editor component

The swell's text editor component allows to embed a full-featured text editor in a Web page that already uses the swell API.

An editor is attached to a particular DIV of a page:

```html
<div id="editor">
	<!-- swell's text fields cab be displayed here -->
</div>
```

```js
var editor = swell.Editor.get("editor");
```

An editor manages swell's text objects. A text can be just local...

```js
var text = swell.Text.create("Some initial text");
```

... or can be shared in a collaborative object if it is assigned to one:

```js
var text = swell.Text.create("Some initial text");

service.open({ id : "shared-text-object" })
    .then(object => {
		
		if (!object.node('text')) {
			object.set('text', text);
		}

		text = object.get('text'); // remind to get this reference!
	});
```

Common editor operations are:

**editor.set(text)**

Attach the text to the editor, the content is rendered

**editor.edit(boolean)**

Enable user editing

**editor.isEditing()**

Checks whether the editor is in user editing mode

**editor.hasDocument()** 

Checks whether the editor has an attached text object

**editor.clean()**

Deattach the text from the editor an clean editor content



## Text selections

The editor component allow apps to assign logic when editor's caret position changes or the user selects text.


```js
editor.setSelectionHandler((range, editor, selection) => {
    


      });
```

The selection handler receives following arguments:

*editor*, a reference to the editor's object.

*range*, see *selection.range*. 

*selection*, the object having information of the current selection. It can be also obtained programatically:


```js
var selection = editor.getSelection();
```

A selection object has following properties and methods:

*selection.anchorNode*, DOM node in which the selection begins.

*selection.anchorOffset*, number representing the offset of the selection's anchor within the anchorNode. If anchorNode is a text node, this is the number of characters within anchorNode preceding the anchor. Zero otherwise.

*selection.focusNode*, DOM node in which the selection ends.

*selection.focusOffset*, number representing the offset of the selection's anchor within the focusNode. If focusNode is a text node, this is the number of characters within focusNode preceding the focus. Zero otherwise.


*selection.range*, start and end indexes of the selection refering to the document content chars, not the DOM rendering. For example:

```js
 { 
	 start: 11,
	 end: 25
 }
```

*selection.isCollpased*, boolean value, true iff range has same start and end positions.

*selection.getFocusBound()*, gets the y-bounds of the cursor position.


*selection.getSelectionPosition()*, coordinates of the current selection start relative to a provided element, for example:

```js
{
	left: 798,
	top: 274,
	offsetParent: <DOM element>
}
```

*selection.getAnchorPosition()*, coordinates of the current selection end relative to a provided element.


## Annotations

An annotation is a pair *{key, value}* associated with a segment of text or *range* of the document. 

```
	It was one of those March days when...
	|                   |   |					  
	0                  20   24 	  
```
In this example, we could set an annotation to the word "March" with the following data:

- *range: (20,24)*
- *name/key: month*
- *value: 4*

The value and the key fields are strings and the range is an ordered pair of positions inside the text.

Annotations can also modify how text is rendered in order to provide visual effects:

### Out of the box annotations

The Swell editor provides a predefined set of annotations that helps to control appearence and styling of the text, following the CSS rules:

**Paragraph style annotations**

| Name | Values |
|----- | ------ |
|header| *h1...h5* |
|textAlign| *left, center, right, justify* |
|list     | *decimal, unordered* |
|indent	  | *outdent, indent* |

**Text style annotations**

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

In order to use annotations that are rendered in a custom way we must define then in the editor:

```js
swell.Editor.AnnotationRegistry.define(
	'comment', 
	'commentclass', 
	{ 
		backgroundColor: grey 
	});
```

In this example, when a *comment* annotation is rendered the editor will render the annotated text with a HTML like this:

```html
	<span class="comment" style="background-color: grey">bla bla bla</span>
```

**Important**: define custom annotations always BEFORE to get a new editor's instance with any of the methods *swell.Editor.createXXX()*


### Working with annotations


**editor.setAnnotation(key, value, [range])**

Sets a new *name* annotation in the provided range with a given *value*.
Setting a *null* value removes the annotation only in given range, spliting the rest of the annotation.

If no range is provided, current selection is assumed.

**editor.clearAnnotation(keys, [range])**

Remove all annotations within array *keys* that are partially o totally contained in the range.

If no range is provided, current selection is assumed.

**editor.getAnnotations(keys, [range])**

Get all annotation values for the given keys. 

If *range* is not provided, current selection range is used.

**editor.getAnnotationsWithValue(keys, value, [range])**

Similar to previous function but filters out annotations with the given value.

*Annotation values*

Annotations obtained from *getAnnotationsXXX()* methods returns an object with an array for each annotation found, for example:

```js
{
	link: [ ... ],
	mark: [ ... ],
	style/fontSize: [ {

		key: 'style/fontSize',
		value: '20px',
		line: <dom element>,
		node: <dom node>,
		range: {
			start: 10,
			end: 21
		},
		text: 'hello world',
		searchMatch: 0


	} ]

}
```
*searchMatch* value can take these values:

- *swell.Annotation.MATCH_IN* the search range is equals or inside the annotations's range.
- *swell.Annotation.MATCH_OUT* the search range is partially out of the annotation's range or spans beyond it.

*Ranges*

Usually you won't need to provide a hand written range (because you will get one from another part of the API) but if that is the case, create a new object with following properties:

```js
var range = {
	start: 11,
	end: 23
};
```

In situations when you need to indicate a range spaning the whole document use the constant:

```js
var fullDocumentRange = swell.Editor.RANGE_ALL;
```




### Overlapping Text annotations

Overlapping of annotations with same name can be problematic for some specific logics. Let's imagine the following example of a annotated text representing a comment (with value A):

```
              	  {comment,A}  
                  |         |
	It was one of those March days when...
	              					  
	                	  
```

If we want to comment another part of the text that overlaps first comment's text, by default, the editor will split an override the former annotation with the new one (B):

```
              	  {comment,A}  
                  |    |
    It was one of those March days when...
	                    |        |              					  
	                    {comment,B}
```

In this case, we have lost the reference of comment A in the word "March". 

To avoid this situation you can use specific methods for text annotations:

**editor.setAnnotationOverlap(name, value, [range])**

For the previous example, this method will generate following annotations:

```
                {comment,A}  {comment,B}  
                  |    |      |  |
    It was one of those March days when...
	                    |    |  
	                  {comment, A,B}
```

Now, the word "March" will be annotated with annotation "comment" with combined value "A,B".

In order to revert this annotation properly use the method:

**editor.clearAnnotationOverlap(name, value, [range])**


### Events

Annotation events are triggered both when the annotation is created or deleted in the document and when it is rendered or removed from the DOM view.

 Annotation's event handler must be set right after the an annotation is defined:

```js
swell.Editor.AnnotationRegistry
	.define("mark","mark");

 swell.Editor.AnnotationRegistry
 	.setHandler("mark", (event) => {

      console.log("annotation event ["+event.type+"] "+event.annotation.key+"="+event.annotation.value+", "+event.annotation.range.start+":"+event.annotation.range.end);

    });	
```
An event object is passed to the handler anytime the given annotation throws an event. This object has following properties:

```js
event = {

	type: <int>,
	annotation: <annotation value>
	domEvent: <dom event>

}
```

There are two type of events, for DOM and for document changes. DOM events are thrown anytime the editor needs to repaint annotations:

- *swell.Annotation.EVENT_DOM_MUTATED* 
- *swell.Annotation.EVENT_DOM_CREATED*
- *swell.Annotation.EVENT_DOM_REMOVED*

Document events are only thrown when annotations are created or removed in the document:

- *swell.Annotation.EVENT_CREATED*
- *swell.Annotation.EVENT_REMOVED*


### Custom carets

Rendering of carets can be customized. Custom carets must be objects providing following properties and methods:

```js
var caret = new Caret(); // our custom caret type
caret.element; // property with the DOM element of the rendered caret.
caret.update(caretInfo); // a method to pass info to the object.
```
Configure the editor component with a factory of carets using the configuration property "caretFactory":

```js
swell.Editor.configure({
	caretFactory: function() { return new Caret(); }
});
```

## Development

### Logging editor events

Log to browser's console:

```js
swell.Editor.configure({
	consoleLog: true
});
```

Log to an HTML panel:

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

Editor sandbox is a small GWT app to play with editor component. Run with following commands:

```sh
./gradlew editorSandbox
```

```
http://127.0.1.1:9876/org.swellrt.sandbox.editor.Editor/editor.html
```

Sandbox editor runs solely in client side, 
no running server is required. 

### Developer's recipes

*Running GWT compiler and DevMode from cmd line*

Look for the `gwt-dev.jar` in local maven repo `find ~/.gradle/caches/ | grep gwt | grep .jar`

`java -cp gwt-dev.jar com.google.gwt.dev.Compiler`

