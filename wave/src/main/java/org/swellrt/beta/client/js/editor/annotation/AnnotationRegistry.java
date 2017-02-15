package org.swellrt.beta.client.js.editor.annotation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.swellrt.beta.client.js.JsUtils;
import org.swellrt.beta.client.js.editor.SEditorException;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.doodad.annotation.GeneralAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.LineStyle;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphBehaviour;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

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
@JsType(namespace = "swellrt.Annotation", name = "Registry")
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
  
  public static final String LINK = AnnotationConstants.LINK_PREFIX;
  
  private static final JsoView CANONICAL_NAMES = JsoView.create(); 
  
  private final static Map<String, Annotation> store = new HashMap<String, Annotation>();
  
  protected static boolean muteHandlers = true;
  
  /**
   * Define friendly names for annotation referencing in SEditor
   */
  static {
    
    //
    // Map annotation names without prefix to their canonical name
    // for the Wave system.
    //
    CANONICAL_NAMES.setString("header", PARAGRAPH_HEADER);
    CANONICAL_NAMES.setString("textAlign", PARAGRAPH_TEXT_ALIGN);
    CANONICAL_NAMES.setString("list", PARAGRAPH_LIST);
    CANONICAL_NAMES.setString("indent", PARAGRAPH_INDENT);
    
    CANONICAL_NAMES.setString("backgroundColor", STYLE_BG_COLOR);
    CANONICAL_NAMES.setString("color", STYLE_COLOR);
    CANONICAL_NAMES.setString("fontFamily", STYLE_FONT_FAMILY);
    CANONICAL_NAMES.setString("fontSize", STYLE_FONT_SIZE);
    CANONICAL_NAMES.setString("fontStyle", STYLE_FONT_STYLE);
    CANONICAL_NAMES.setString("fontWeight", STYLE_FONT_WEIGHT);
    CANONICAL_NAMES.setString("textDecoration", STYLE_TEXT_DECORATION);
    CANONICAL_NAMES.setString("verticalAlign", STYLE_VERTICAL_ALIGN);
    
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
    
    store.put(PARAGRAPH_HEADER, new ParagraphValueAnnotation(ParagraphBehaviour.HEADING, PARAGRAPH_HEADER, m, new Annotation.AttributeGenerator() {

      @Override
      public Map<String, String> generate(Range range, String styleKey) {
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
    m.put("default", Paragraph.Alignment.LEFT);
    
    store.put(PARAGRAPH_TEXT_ALIGN, new ParagraphValueAnnotation(ParagraphBehaviour.DEFAULT, PARAGRAPH_TEXT_ALIGN, m, null));
   
    //
    // Paragraph List
    //
    
    m = new HashMap<String, LineStyle>();
    m.put("decimal", Paragraph.listStyle(Paragraph.LIST_STYLE_DECIMAL));
    m.put("unordered", Paragraph.listStyle(null));
    m.put("default", Paragraph.listStyle(null));
    
    store.put(PARAGRAPH_LIST, new ParagraphValueAnnotation(ParagraphBehaviour.LIST, PARAGRAPH_LIST, m, null));
    
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
    
    store.put(AnnotationConstants.LINK_PREFIX, new TextAnnotation(AnnotationConstants.LINK_PREFIX));
  }
  
  @JsIgnore
  public static Set<String> getNames() {
    return store.keySet();
  }
  
  @JsIgnore
  public static void forEach(BiConsumer<String, Annotation> action) {
    store.forEach(action);
  }
  
  @JsIgnore
  public static Annotation get(String name) {  
    String canonicalName = CANONICAL_NAMES.getString(name);
    return store.get(canonicalName != null ? canonicalName : name);
  }
  
  
  protected static boolean isParagraphAnnotation(String name) {
    String canonicalName = CANONICAL_NAMES.getString(name);
    name = canonicalName != null ? canonicalName : name;
    return name.startsWith("paragraph/");
  }
  
  /**
   * Define a new custom annotation.
   * 
   * @param name annotation's name
   * @param cssClass a css class for the html container
   * @param cssStyle css styles for the html container
   */
  public static void define(String name, String cssClass, JavaScriptObject cssStyleObj) throws SEditorException {    
    
    if (name == null || name.startsWith("paragraph") || name.startsWith(AnnotationConstants.STYLE_PREFIX) || CANONICAL_NAMES.getString(name) != null) {
      throw new SEditorException("Not valid annotation name");
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
  public static void setHandler(String name, AnnotationInstance.Handler handler) {    
    Annotation antn = get(name);
    
    if (antn != null && antn instanceof Annotation.Listenable) {
      Annotation.Listenable ta = (Annotation.Listenable) antn;   
      ta.setHandler(handler);
    }
  }
  
  /**
   * Clear the handler for events on one annotation type
   * @param name
   * @param handler
   */
  public static void unsetHandler(String name) {    
    Annotation antn = get(name);
    
    if (antn != null && antn instanceof Annotation.Listenable)  {
      Annotation.Listenable ta = (Annotation.Listenable) antn;   
      ta.setHandler(null);
    }
  }
    
  
  public static void muteHandlers(boolean mute) {
    muteHandlers = mute;
  }
}
