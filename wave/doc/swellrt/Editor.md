(This file is outdated)
# SwellRT Editor Web Component

## Editor API


### Annotations

*Create / Update*

setAnnotation(String key, String value):JsoAnnotation (OK)
setAnnotationInRange(JsoEditorRange range, String key, String value); (OK)

*Get*

getAnnotationSet(): (OK)
getAnnotationInRange(): (OK)

getSelection(): (OK)

note: to get all annotations in a the current selection call getSelection().

*Remove*

clearAnnotation(String prefixKey) (OK)
clearAnnotationsInRange(JsoEditorRange editorRange, prefixKey) (OK)


### Text

setText(JsoEditorRange range, String text): void
getText(JsoEditorRange range): String
deleteText(JsoEditorRange range): void


### Annotations


*Controller:*

  styleClass: <String>,
  style: <Object>,   
  onEvent: function(range, event),
  onChange: function(range),


 *Types:*

 	style/* (not support for controllers)
 	paragraph/* (not support for controllers)
 	paragraph/header
 	link 	
 	Custom annotations



Outline
- obtener todas las anotaciones header
- saber cuando cambia el texto/contenido de la anotación

	Sem:

	onChange -> getAnnotationSet
	onAdd / onRemove -> getAnnotationSet

Enlances
- mostrar popup al insertar link y cambiar el texto y el valor de la anotación insertada
- en click mostrar popup cambiar texto y valor de la anotación



Comentarios
- Obtener todos los comentarios



### Widgets


## Development

### Profiles




### Using Wave Editor version

- Run `./gradlew gwtEditorDev`
- Navigate to `http://localhost:9876/org.waveprotocol.wave.client.editor.harness.EditorTest/EditorTest.html`
- Activate GWT SuperDev mode if necessary with the provided links

Configure extension of the editor in `DefaultTestHarness.java`



### Debugging SwellRT TextEditor component code

To enable browser logs set the following flag: `window.__debugEditor = true`;



### GWT

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
