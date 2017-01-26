package org.swellrt.beta.client.js.editor.annotation;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.swellrt.beta.client.js.JsUtils;
import org.swellrt.beta.client.js.editor.SEditor;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.doodad.annotation.GeneralAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.LineStyle;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * Putting together all annotation stuff 
 * to ease handling in the SwellRT editor component.
 * <p>
 * <br>
 * List of text annotation handlers:
 * <p>
 * <li>{@link StyleAnnotationHandler}: handler for style annotations</li>
 * <li>{@link LinkAnnotationHandler}: handler for link annotations</li>
 * <li>{@link GeneralAnnotationHandler}: handler for custom annotations</li>
 * <p><br>
 * Rendering logic for previous annotations are in following classes:
 * <p>
 * <li>{@link AnnotationPaint}: utility methods and constants for annotation renderers</li>
 * <li>{@link AnnotationSpreadRenderer}: html renderer for style and custom annotations</li>
 * 
 * <p><br>
 * For Paragraph annotations check out {@link Paragraph} class.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swellrt.Editor", name = "AnnotationRegistry")
public class AnnotationRegistry {

  public static final String PARAGRAPH_HEADER = "paragraph/header";
  public static final String PARAGRAPH_TEXT_ALIGN = "paragraph/textAlign";
  public static final String PARAGRAPH_LIST = "paragraph/list";
  public static final String PARAGRAPH_INDENT = "paragraph/indent";
  
  public static final String STYLE_BG_COLOR = AnnotationConstants.STYLE_BG_COLOR;
  public static final String STYLE_COLOR = AnnotationConstants.STYLE_COLOR;
  public static final String STYLE_FONT_FAMILY = AnnotationConstants.STYLE_FONT_FAMILY;
  public static final String STYLE_FONT_SIZE = AnnotationConstants.STYLE_FONT_SIZE;
  public static final String STYLE_FONT_STYLE = AnnotationConstants.STYLE_FONT_STYLE;
  public static final String STYLE_FONT_WEIGHT = AnnotationConstants.STYLE_FONT_WEIGHT;
  public static final String STYLE_TEXT_DECORATION = AnnotationConstants.STYLE_TEXT_DECORATION;
  public static final String STYLE_VERTICAL_ALIGN = AnnotationConstants.STYLE_VERTICAL_ALIGN;
  
  public static class AnnotationBulkAction {
    
    Set<Annotation> paragraphAnnotations = new HashSet<Annotation>(); 
    JsoStringSet textAnnotations = JsoStringSet.create(); 

    final Range range;
    final EditorContext editor;
    final SEditor seditor;
    
    public AnnotationBulkAction(SEditor seditor, EditorContext editor, Range range) {
      this.range = range;
      this.editor = editor;
      this.seditor = seditor;
    }
    
    public void add(JsArrayString names) {
      for (int i = 0; i < names.length(); i++)
        add(names.get(i));
    }
    
    public void add(String nameOrPrefix) {
      Preconditions.checkArgument(nameOrPrefix != null && !nameOrPrefix.isEmpty(),
          "Annotation name or prefix not provided");

      if (!nameOrPrefix.contains("/")) { // it's name
        addByName(nameOrPrefix);
      } else { // it's prefix
        for (String name : store.keySet()) {
          if (name.startsWith(nameOrPrefix)) {
            addByName(name);
          }
        }

      }
    }
    
    protected void addByName(String name) {
      Annotation antn = store.get(name);
      if (antn != null) {
       
        if (antn instanceof TextAnnotation) 
          textAnnotations.add(name);
        else
          paragraphAnnotations.add(antn);
        
      }   
    }
    
    protected void addAllAnnotations() {
      store.forEach(new BiConsumer<String, Annotation>() {

        @Override
        public void accept(String t, Annotation u) {
          
          if (u instanceof TextAnnotation) {
            textAnnotations.add(t);            
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
      
      if (!textAnnotations.isEmpty()) {
        String[] array =  JsUtils.stringSetToArray(textAnnotations);
        EditorAnnotationUtil.clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(), array, range.getStart(), range.getEnd());
      }
      
      for (Annotation antn: paragraphAnnotations)
        antn.reset(editor, range);
    }
    
    protected int getRangeMatchType (Range selectionRange, Range annotationRange) {      
      boolean in = selectionRange.equals(annotationRange) || (annotationRange.getStart() <= selectionRange.getStart() &&  selectionRange.getEnd() <= annotationRange.getEnd());     
      return in ? AnnotationInstance.MATCH_IN : AnnotationInstance.MATCH_OUT;
    }
    
    protected JsoView createResult() {
      return JsoView.as(JavaScriptObject.createObject());
    }
    
    protected void addToResult(JsoView result, String key, AnnotationInstance instance) {
      JavaScriptObject arrayJso = result.getJso(key);
      if (arrayJso == null) {
        arrayJso = JsoView.createArray().cast();
        result.setJso(key, arrayJso);
      }     
      JsUtils.addToArray(arrayJso, instance);
    }
    
    protected Node lookupNode(ContentNode n) {
      if (n != null) {
          Node rightwardsNodelet = n.getImplNodeletRightwards();
          if (rightwardsNodelet != null) {
            return rightwardsNodelet;
          }
      }
      
      return null;
    }
    
    
    public JavaScriptObject get() {
      
      JsoView result = createResult();

      boolean getAll = textAnnotations.isEmpty() && paragraphAnnotations.isEmpty();
      
      if (getAll) 
        addAllAnnotations();
      
      //
      // Text (caret) annotations
      // 
      
      if (range.isCollapsed())  {
      
        editor.getDocument().forEachAnnotationAt(range.getStart(), new ProcV<String>() {

          @Override
          public void apply(String key, String value) {
            
            if ((getAll || textAnnotations.contains(key)) && !isParagraphAnnotation(key)) {
              
              Range actualRange = EditorAnnotationUtil.getEncompassingAnnotationRange(editor.getDocument(), key , range.getStart());
              Point<ContentNode> atNode = editor.getDocument().locate(range.getStart()+1);
              ContentElement lineNode = LineContainers.getRelatedLineElement(editor.getDocument(), atNode);
              
              Element lineElement = lookupNode(lineNode).getParentElement();
              Node node = atNode.getContainer().getImplNodelet().getParentElement();         
              
              addToResult(result, key, new AnnotationInstance(seditor, key, value, "", actualRange, lineElement, node, getRangeMatchType(range, actualRange)));
            }
          }
        });
      
      } else {
        
        ReadableStringSet keys = !getAll ? textAnnotations : null;
        editor.getDocument().rangedAnnotations(range.getStart(), range.getEnd(), keys).forEach(new Consumer<RangedAnnotation<String>>() {
          @Override
          public void accept(RangedAnnotation<String> t) {
            if (t.value() != null) {
              Range r = new Range(t.start(), t.end());
              String text = DocHelper.getText(editor.getDocument(), t.start(), t.end());
              Point<ContentNode> atNode = editor.getDocument().locate(t.start()+1);
              ContentElement lineNode = LineContainers.getRelatedLineElement(editor.getDocument(), atNode);
              
              Element lineElement = lookupNode(lineNode).getParentElement();
              Node node = atNode.getContainer().getImplNodelet().getParentElement();    
              
              addToResult(result, t.key(), new AnnotationInstance(seditor, t.key(), t.value(), text, r, lineElement, node, getRangeMatchType(range, r)));
            }             
          }          
        });
        
      }
  
      //
      // Paragraph annotations
      // 

      String text = DocHelper.getText(editor.getDocument(), range.getStart(), range.getEnd());
      Point<ContentNode> linePoint = editor.getDocument().locate(range.getStart()+1);
      ContentNode lineNode = LineContainers.getRelatedLineElement(editor.getDocument(), linePoint);
      Element lineElement = lineNode.asElement().getImplNodeletRightwards().getParentElement();     
       
      for (Annotation antn: paragraphAnnotations) {
        
        if (antn instanceof ParagraphValueAnnotation) {
          String name = ((ParagraphValueAnnotation) antn).getName();
          String value = ((ParagraphValueAnnotation) antn).apply(editor, range);
          if (value != null) {
            addToResult(result,name, new AnnotationInstance(seditor, name, value, text, range, lineElement, null, AnnotationInstance.MATCH_IN));
          }
        }
      }
      
      return result;
    }
    
  }
  
  
  
  
  
  private final static Map<String, Annotation> store = new HashMap<String, Annotation>();
  
  
  /**
   * Define friendly names for annotation referencing in SEditor
   */
  static {
    
    //
    // Paragraph Headers
    //

    Map<String, LineStyle> m = new HashMap<String, LineStyle>();
    m.put("h1", Paragraph.regularStyle("h1"));
    m.put("h2", Paragraph.regularStyle("h2"));
    m.put("h3", Paragraph.regularStyle("h3"));
    m.put("h4", Paragraph.regularStyle("h4"));
    m.put("h5", Paragraph.regularStyle("h5"));
    m.put("default", Paragraph.regularStyle(""));
    
    store.put(PARAGRAPH_HEADER, new ParagraphValueAnnotation(PARAGRAPH_HEADER, m, new Annotation.AttributeGenerator() {

      @Override
      public Map<String, String> generate(EditorContext editor, Range range, String styleKey) {
        // This code auto generates an id for each header! so we can reference them in the DOM
        Date now = new Date();
        String id = String.valueOf(now.getTime()) + 
                    String.valueOf(range.getStart()) +
                    String.valueOf(range.getEnd());
        return CollectionUtils.<String, String> immutableMap(Paragraph.ID_ATTR, id);
      }
    }));

    //
    // Paragraph Text alignment
    //
    
    m = new HashMap<String, LineStyle>();
    m.put("left", Paragraph.Alignment.LEFT);
    m.put("center", Paragraph.Alignment.CENTER);
    m.put("right", Paragraph.Alignment.RIGHT);
    m.put("justify", Paragraph.Alignment.JUSTIFY);
    m.put("default", null);
    
    store.put(PARAGRAPH_TEXT_ALIGN, new ParagraphValueAnnotation(PARAGRAPH_TEXT_ALIGN, m, null));
   
    //
    // Paragraph List
    //
    
    m = new HashMap<String, LineStyle>();
    m.put("decimal", Paragraph.listStyle(Paragraph.LIST_STYLE_DECIMAL));
    m.put("unordered", Paragraph.listStyle(null));
    m.put("default", Paragraph.listStyle(null));
    
    store.put(PARAGRAPH_LIST, new ParagraphValueAnnotation(PARAGRAPH_TEXT_ALIGN, m, null));
    
    //
    // Paragraph indentation
    //
    
    Map<String, ContentElement.Action> ma = new HashMap<String, ContentElement.Action>();
    ma.put("outdent", Paragraph.OUTDENTER);
    ma.put("indent", Paragraph.INDENTER);
    
    store.put(PARAGRAPH_INDENT, new ParagraphActionAnnotation(ma, Paragraph.RESET_INDENT));
    
    //
    // Style Annotations
    // 
    
    store.put(AnnotationConstants.STYLE_BG_COLOR, new TextAnnotation(AnnotationConstants.STYLE_BG_COLOR));
    store.put(AnnotationConstants.STYLE_COLOR, new TextAnnotation(AnnotationConstants.STYLE_COLOR));
    store.put(AnnotationConstants.STYLE_FONT_FAMILY, new TextAnnotation(AnnotationConstants.STYLE_FONT_FAMILY));
    store.put(AnnotationConstants.STYLE_FONT_SIZE, new TextAnnotation(AnnotationConstants.STYLE_FONT_SIZE));
    store.put(AnnotationConstants.STYLE_FONT_STYLE, new TextAnnotation(AnnotationConstants.STYLE_FONT_STYLE));
    store.put(AnnotationConstants.STYLE_FONT_WEIGHT, new TextAnnotation(AnnotationConstants.STYLE_FONT_WEIGHT));
    store.put(AnnotationConstants.STYLE_TEXT_DECORATION, new TextAnnotation(AnnotationConstants.STYLE_TEXT_DECORATION));
    store.put(AnnotationConstants.STYLE_VERTICAL_ALIGN, new TextAnnotation(AnnotationConstants.STYLE_VERTICAL_ALIGN));
    
  }
  
  @JsIgnore
  public static Annotation get(String name) {
    return store.get(name);
  }
  
  
  protected static boolean isParagraphAnnotation(String name) {
    return name.startsWith("paragraph/");
  }
  
  /**
   * Define a new custom annotation.
   * 
   * @param name annotation's name
   * @param cssClass a css class for the html container
   * @param cssStyle css styles for the html container
   */
  public static void define(String name, String cssClass, JavaScriptObject cssStyleObj) {    
    
    if (name == null || name.startsWith("paragraph") || name.startsWith(AnnotationConstants.STYLE_PREFIX)) {
      return;
    }
    
    JsoView styles = null;
    if (JsUtils.isString(cssStyleObj)) {
      JavaScriptObject o = JsonUtils.unsafeEval(cssStyleObj.toString());
      if (o != null) {
        styles = JsoView.as(o);
      }
    } else {
      styles = JsoView.as(cssStyleObj);
    }
    
    store.put(name, new CustomTextAnnotation(name));
    GeneralAnnotationHandler.register(Editor.ROOT_REGISTRIES, name, cssClass, styles);    
  }

  /**
   * Set a handler for events on one annotation type
   * @param name
   * @param handler
   */
  public static void setHandler(String name, AnnotationEventHandler handler) {    
    Annotation antn = store.get(name);
    
    if (antn != null && antn instanceof TextAnnotation) {
      TextAnnotation ta = (TextAnnotation) antn;   
      ta.setHandler(handler);
    }
  }
  
  /**
   * Clear the handler for events on one annotation type
   * @param name
   * @param handler
   */
  public static void unsetHandler(String name) {    
    Annotation antn = store.get(name);
    
    if (antn != null && antn instanceof TextAnnotation) {
      TextAnnotation ta = (TextAnnotation) antn;   
      ta.setHandler(null);
    }
  }
  
}
