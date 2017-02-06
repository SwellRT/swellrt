package org.swellrt.beta.client.js.editor.annotation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.swellrt.beta.client.js.JsUtils;
import org.swellrt.beta.client.js.editor.SEditorException;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

/**
 * Action to be performed in a set of annotations for a provided range.
 * 
 * TODO rewrite in a stream-based way, current impl is a mess.
 * 
 *  @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class AnnotationAction {
  
  Set<Annotation> paragraphAnnotations = new HashSet<Annotation>(); 
  Set<TextAnnotation> textAnnotations = new HashSet<TextAnnotation>(); 
  JsoStringSet textAnnotationsNames = JsoStringSet.create();

  final Range range;
  final EditorContext editor;
  
  boolean projectEffective = false;
  
  public AnnotationAction(EditorContext editor, Range range) {
    this.range = range;
    this.editor = editor;
  }
     
  public void add(JsArrayString names) throws SEditorException {
    for (int i = 0; i < names.length(); i++)
      add(names.get(i));
  }
  
  public void add(String nameOrPrefix) throws SEditorException {
    Preconditions.checkArgument(nameOrPrefix != null && !nameOrPrefix.isEmpty(),
        "Annotation name or prefix not provided");

    if (!nameOrPrefix.contains("/")) { // it's name
      addByName(nameOrPrefix);
    } else { // it's prefix
      for (String name : AnnotationRegistry.getNames()) {
        if (name.startsWith(nameOrPrefix)) {
          addByName(name);
        }
      }

    }
  }
  

  /**
   * Configure this annotation action to only consider effective annotations for its range.
   * <p>
   * Within a range, there could exist multiple instances of the same annotation spanning
   * subranges. An effective annotation must span at least the size of the range.   
   * 
   * @param projectEffective enable projections o
   */
  public void onlyEffectiveAnnotations(boolean enable) {
    this.projectEffective = enable;
  }
  
  protected void addByName(String name) throws SEditorException {
    Annotation antn = AnnotationRegistry.get(name);
    if (antn != null) {
     
      if (antn instanceof TextAnnotation) {
        textAnnotations.add((TextAnnotation) antn);
        textAnnotationsNames.add(((TextAnnotation) antn).getName()); // ensure we use canonical name
      } else
        paragraphAnnotations.add(antn);
      
    }  else{
      throw new SEditorException("Invalid annotation name");
    }
  }
  
  protected void addAllAnnotations() {
    AnnotationRegistry.forEach(new BiConsumer<String, Annotation>() {

      @Override
      public void accept(String t, Annotation u) {
        
        if (u instanceof TextAnnotation) {
          textAnnotations.add((TextAnnotation) u);
          textAnnotationsNames.add(t);
        } else if (u instanceof ParagraphValueAnnotation) {
          paragraphAnnotations.add(u);
        }
        
      }
    });
  }
  
  public void reset() {
    
    boolean getAll = textAnnotations.isEmpty() && paragraphAnnotations.isEmpty();
    
    if (getAll) 
      addAllAnnotations();
    
    // Text annotations
    EditorAnnotationUtil.clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(), textAnnotationsNames, range.getStart(), range.getEnd());
    
    // Paragraph annotations
    for (Annotation antn: paragraphAnnotations)
      antn.reset(editor, range);
  }
  

  
  protected JsoView createResult() {
    return JsoView.as(JavaScriptObject.createObject());
  }
  
  protected void addToResult(JsoView result, String key, AnnotationInstance instance) {
    
    //
    // Remove prefix of annotation key to avoid javascript syntax errors
    // accessing properties e.g. "annotations.paragraph/header" is not supported
    //
    
    int slashPos = key.indexOf("/");
    if (slashPos >= 0)
      key = key.substring(slashPos+1);
                
    
    if (!projectEffective) {
      
      // Map of arrays, we expect multiple instances of the same annotation within a range.        
      
      JavaScriptObject arrayJso = result.getJso(key);      
      if (arrayJso == null) {
        arrayJso = JsoView.createArray().cast();
        result.setJso(key, arrayJso);
      }        
      JsUtils.addToArray(arrayJso, instance);
      
    } else {
      
      // on projecting effective annotations, if several instances
      // are found in the range, keep always the one with a non null value.
      boolean overwriteAnnotation = true;
         
      Object formerInstance = result.getObjectUnsafe(key);
      if (formerInstance != null) {
        AnnotationInstance typedFormerInstance = (AnnotationInstance) formerInstance;
        overwriteAnnotation = typedFormerInstance.getValue() == null;
      }
      
      // A simple map, one annotation instance per key     
      if (overwriteAnnotation)
        result.setObject(key, instance);        
    }
  }
  
  
  /** 
   * @return annotations object 
   */
  public JavaScriptObject get() {
    
    JsoView result = createResult();

    boolean getAll = textAnnotations.isEmpty() && paragraphAnnotations.isEmpty();
    
    if (getAll) 
      addAllAnnotations();
    
    //
    // Text annotations
    // 
    
    if (range.isCollapsed()) {
      
      for (TextAnnotation antn: textAnnotations) {
        String value = editor.getDocument().getAnnotation(range.getStart(), antn.getName());
        Range actualRange = EditorAnnotationUtil.getEncompassingAnnotationRange(editor.getDocument(), antn.getName(), range.getStart());
        if (value != null)
          addToResult(result, antn.getName(), AnnotationInstance.create(editor.getDocument(), antn.getName(), value, actualRange, AnnotationInstance.MATCH_IN)); 
      }
      
    } else {
     
      //
      // Within a range, there could exist multiple instances of the same annotation. 
      // If projectEffective, only consider those spanning at least the full range.
     
      editor.getDocument().rangedAnnotations(range.getStart(), range.getEnd(), textAnnotationsNames).forEach(new Consumer<RangedAnnotation<String>>() {
        @Override
        public void accept(RangedAnnotation<String> t) {
          Range anotRange = new Range(t.start(), t.end());          
          int matchType = AnnotationInstance.getRangeMatch(range, anotRange);            
           
          if (projectEffective && matchType != AnnotationInstance.MATCH_IN)
            return;
            
          if (t.value() != null)
              addToResult(result, t.key(), AnnotationInstance.create(editor.getDocument(), t.key(), t.value(), anotRange, matchType));          
        }          
      });
    }
    
    //
    // Paragraph annotations
    // 
     
    for (Annotation antn: paragraphAnnotations) {
      
      if (antn instanceof ParagraphValueAnnotation) {
        String name = ((ParagraphValueAnnotation) antn).getName();
        String value = ((ParagraphValueAnnotation) antn).apply(editor, range);
        if (value != null)
          addToResult(result,name, new AnnotationInstance(editor.getDocument(), name, value, range, null, AnnotationInstance.MATCH_IN));   
      }
    }
    
    return result;
  }
  
}
