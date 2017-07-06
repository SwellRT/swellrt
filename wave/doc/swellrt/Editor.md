

Swell Web API includes a rich-text editor highly customizable.



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


*editor.setAnnotation(name, value, [range])* 

Sets a new "name" annotation in the provided range with "value".

*editor.clearAnnotation(names, [range])*

Remove all annotations with name in the "names" array or object that are partially o totally contained in the range.


If *range* is ommitted the editor will assume the one from the current selected text.

*Ranges*

Usually you won't need to provide a hand written range (because you will get one from another part of the API) but if that is the case, create a new range object with:

*var range = swell.Editor.Range.create(start, end);*

In situations when you need to indicate a range spaning the whole document use this constant:

*var rangeAllText = swell.Editor.Range.ALL;*


### Text annotations and overlapping

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

*editor.setTextAnnotationOverlap(name, value, [range])*

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

*editor.clearTextAnnotationOverlap(name, value, [range])*


### Getting annotations

*editor.seekTextAnnotations(names, [range], [onlyWithinRange])*

Get all annotations matching a name. Optionally
a range can be specified and a flag to only retrieve annotations fully withing that range.

*names* argument can be a single string or an array of strings.

*editor.seekTextAnnotationsByValue(name, value, [range])*

Get all annotations matching the provided name and value. Optionally
a range can be specified.

The result object of these methods has a different array of *annotation instances* for each annotation name.

```js
{ 
	annotation_name_1 : [ <array of annotation instances> ],
	...
	annotation_name_N : [ <array of annotation instances> ]
}
```

### Annotation instances

An *Annotation Instance* has the information of one annotation in a particular range of text, and it provides some methods to manipulate it:

```js

var annotationInstance = ...

annotationInstance.range; // range.start, range.end
annotationInstance.name; 
annotationInstance.value;
annotationInstance.text;
annotationInstance.matchType; // how seek range matches annotation's range

}

```

Match type can take following values:

- *swell.Annotation.MATCH_IN* the seek range is equals or inside the annotations's range.
- *swell.Annotation.MATCH_OUT* the seek range is partially out of the annotation's range or spans beyond it.


An instance have following methods:

*annotationInstance.clear()*

Delete the annotation. The object can't be used any more.

*annotationInstance.getLine()*

Returns the DOM element of the nearest line containing the annotated text.

*annotationInstance.getNode()*

Returns the DOM element of the annotated text.

*annotationInstance.mutate(text)*

Changes the annotated text, this can change the range.

*annotationInstance.update(value)*

Changes the value of the annotation.

### Events

Create, update and delete events are triggered anytime the editor renders an annotation. This doesn't happen only when the annotation is created or delete, as can be expected. They also happens when the annotation needs to be render again because of different situations, for example, when another annotation affecting the same is rendered.

*swell.Annotation.EVENT_MUTATED* 

- When the document is rendered.
- When text withing the annotation changes.

*swell.Annotation.EVENT_CREATED*

*swell.Annotation.EVENT_REMOVED*
- Range null

## Development

### Show debug messages in Web console

Set the following configuration property before Swell script is loaded:

```
    __swell_editor_config = {
      enableLog: true
    };
```

### Wave Editor 

The Wave project included in Swell has a particular GWT module to try and debug the editor component:


- Run `./gradlew gwtEditorDev`
- Navigate to `http://localhost:9876/org.waveprotocol.wave.client.editor.harness.EditorTest/EditorTest.html`
- Activate GWT SuperDev mode if necessary with the provided links

Configure extension of the editor in `DefaultTestHarness.java`


### Developer's recipes

*Running GWT compiler and DevMode from cmd line*

Look for the `gwt-dev.jar` in local maven repo `find ~/.gradle/caches/ | grep gwt | grep .jar`

`java -cp gwt-dev.jar com.google.gwt.dev.Compiler`

*Issue with GWT 2.8 and source path selectors*

Doesn't work as expected...

```  
  	<source path="" excludes="gson/** proto/**"/>
  	<source path="" excludes="gson/**/*.* proto/**/*.*"/>
  	<source path="" includes="*.*"/>  	
```   	

Get's everything from the starting path...  	

```   	
    <source path=""></source>
```      

Only gets the include file    

```      
    <source path="">
  		<include name="ProfilesProto.java" />
  	</source>
```     
