package org.swellrt.beta.client.platform.web.editor.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.swellrt.beta.client.platform.js.JsUtils;
import org.swellrt.beta.client.platform.web.editor.SEditorException;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.LineStyle;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphBehaviour;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringSet;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A single place to put all annotation related logic.
 * <p>
 * Static methods implement set/unset logic for annotations.
 * <p>
 * Instances of this class are wrappers of each annotation type, merely to track
 * them and manage event handlers.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AnnotationController {

  public interface AttributeGenerator {

    public Map<String, String> generate(Range range, String styleKey);

  }

  public enum Type {

    TEXT, PARAGRAPH_STYLE, PARAGRAPH_ACTION

  }

  //
  //
  //

  /**
   * A single method to set annotations of any type.
   *
   * @param editor
   * @param key
   * @param value
   * @param range
   * @throws SEditorException
   */
  public static AnnotationValue set(Editor editor, String key, final String value, Range range)
      throws SEditorException {

    final AnnotationController ac = AnnotationRegistry.get(key);

    if (ac == null)
      throw new SEditorException(SEditorException.UNKNOWN_ANNOTATION,
          "Annotation key is not registered");

    //
    // Text
    //

    if (ac.getType().equals(AnnotationController.Type.TEXT)) {

      if (Annotations.isLocal(key)) {
        editor.getContent().getLocalAnnotations().setAnnotation(range.getStart(), range.getEnd(),
            key, value);
      } else {
        EditorAnnotationUtil.setAnnotationOverRange(editor.getDocument(),
            editor.getCaretAnnotations(), key, value, range.getStart(), range.getEnd());
      }

    } else if (ac.getType().equals(AnnotationController.Type.PARAGRAPH_STYLE)) {

      //
      // Paragraph value
      //
      applyParagraphStyle(editor, key, value, range);

    } else if (ac.getType().equals(AnnotationController.Type.PARAGRAPH_ACTION)) {

      editor.undoableSequence(new Runnable() {
        @Override
        public void run() {

          ContentElement.Action action = ac.actions.get(value);
          if (action == null)
            return;

          Paragraph.traverse(editor.getContent().getLocationMapper(), range.getStart(),
              range.getEnd(), action);
        }
      });

    }

    return AnnotationValueBuilder.buildWithRange(editor.getContent().getMutableDoc(), key, value,
        range,
        AnnotationValue.MATCH_IN);

  }

  protected static void applyParagraphStyle(Editor editor, String key, String value, Range range) {

    final AnnotationController controller = AnnotationRegistry.get(key);

    if (controller == null)
      return;

    if (value != null && !value.isEmpty() && !controller.styles.containsKey(value))
      return;

    boolean isOn = (value != null && !value.isEmpty() && !value.equals("default"));
    String styleValue = null;
    if (!isOn)
      styleValue = "default";
    else
      styleValue = value;

    Paragraph.LineStyle style = controller.styles.get(styleValue);
    if (controller.attributeGenerator != null)
      style.setAttributes(controller.attributeGenerator.generate(range, styleValue));

    Paragraph.apply(editor.getContent().getLocationMapper(), range.getStart(), range.getEnd(),
        style, isOn);
    style.setAttributes(null);

  }

  public static void clearAnnotation(Editor editor, ReadableStringSet keys, Range range) {

    StringSet paragraphKeySet = CollectionUtils.createStringSet();
    StringSet textKeySet = CollectionUtils.createStringSet();
    filterOutKeys(keys, paragraphKeySet, textKeySet, AnnotationRegistry::isParagraphAnnotation);

    StringSet textLocalKeySet = CollectionUtils.createStringSet();
    StringSet textRemoteKeySet = CollectionUtils.createStringSet();
    filterOutKeys(textKeySet, textLocalKeySet, textRemoteKeySet,  Annotations::isLocal);

    editor.undoableSequence(new Runnable() {

      @Override
      public void run() {

        // Local annotations
        EditorAnnotationUtil.clearAnnotationsOverRange(editor.getContent().getLocalAnnotations(), editor.getCaretAnnotations(), textLocalKeySet, range.getStart(), range.getEnd());

        // Persisted annotations
        EditorAnnotationUtil.clearAnnotationsOverRange(editor.getDocument(),
            editor.getCaretAnnotations(), textRemoteKeySet, range.getStart(), range.getEnd());

        // Paragraph annotations
        paragraphKeySet.each(new Proc() {

          @Override
          public void apply(String key) {

            final AnnotationController ac = AnnotationRegistry.get(key);

            if (ac == null)
              return;

            if (ac.getType().equals(AnnotationController.Type.PARAGRAPH_STYLE)) {

              //
              // Paragraph value
              //
              applyParagraphStyle(editor, key, null, range);

            } else if (ac.getType().equals(AnnotationController.Type.PARAGRAPH_ACTION)) {

              //
              // Paragraph action
              //
              if (ac.resetAction != null) {
                Paragraph.traverse(editor.getContent().getLocationMapper(), range.getStart(),
                    range.getEnd(), ac.resetAction);
              }

            }


          }

        });

      }
    });

  }

  private static void filterOutKeys(ReadableStringSet keySet, StringSet inSet, StringSet outSet,
      Function<String, Boolean> matcher) {
    keySet.each(new Proc() {

      @Override
      public void apply(String key) {

        if (matcher.apply(key)) {
          inSet.add(key);
        } else {
          outSet.add(key);
        }

      }
    });

  }

  /**
   * Adjust a given range in order to be used to get annotations. If range is
   * collapsed, range must be expanded by 1 char in the current paragraph
   *
   * @param editor
   * @param range
   * @return
   */
  private static Range selectionRangeForGetAnnotation(Editor editor, Range range) {

    if (!range.isCollapsed()) {
      return range;
    } else {

      Point<ContentNode> startPoint = editor.getDocument().locate(range.getStart());
      Point<ContentNode> endPoint = editor.getDocument().locate(range.getStart() + 1);

      if (startPoint.getContainer().equals(endPoint.getContainer())) {
        return Range.create(range.getStart(), range.getStart() + 1);
      }

      startPoint = editor.getDocument().locate(range.getStart() - 1);
      endPoint = editor.getDocument().locate(range.getStart());

      if (startPoint.getContainer().equals(endPoint.getContainer())) {
        return Range.create(range.getStart() - 1, range.getStart());
      }

      return range;
    }

  }

  public static JavaScriptObject getAnnotationsWithFilters(Editor editor, JavaScriptObject keys,
      Range range, Function<Object, Boolean> valueMatcher)
      throws SEditorException {

    JsoView result = JsoView.as(JavaScriptObject.createObject());
    ReadableStringSet keySet = AnnotationRegistry.normalizeKeys(JsUtils.toStringSet(keys));
    StringSet paragraphKeySet = CollectionUtils.createStringSet();
    StringSet textKeySet = CollectionUtils.createStringSet();
    filterOutKeys(keySet, paragraphKeySet, textKeySet, AnnotationRegistry::isParagraphAnnotation);

    Range searchRange = selectionRangeForGetAnnotation(editor, range);

    // Note: scan local annotations cause them include remote ones

    // get all annotations, filter later
    editor.getContent().getLocalAnnotations()
        .rangedAnnotations(searchRange.getStart(), searchRange.getEnd(), null)
        .forEach((RangedAnnotation<Object> ra) -> {

          // ignore annotations with null value, are just editor's internal
          // stuff
          if (ra.value() == null)
            return;

          // get all annotations and filter here
          if (!keySet.isEmpty() && !AnnotationRegistry.matchKeys(textKeySet, ra.key()))
            return;

          AnnotationController a = AnnotationRegistry.get(ra.key());
          if (a == null)
            return; // skip not registered annotations


          Range anotRange = new Range(ra.start(), ra.end());
          int rangeMatch = AnnotationValueBuilder.getRangeMatch(searchRange, anotRange);

          if (valueMatcher != null && !valueMatcher.apply(ra.value()))
            return; // skip

          if (!result.containsKey(ra.key())) {
            result.setJso(ra.key(), JavaScriptObject.createArray());
          }

          AnnotationValue anotationValue = AnnotationValueBuilder.buildWithRange(
              editor.getContent().getMutableDoc(), ra.key(), ra.value(), anotRange, rangeMatch);

          JsUtils.addToArray(result.getJso(ra.key()), anotationValue);
        });

    //
    // Paragraph annotations
    //


    List<AnnotationController> controllers = new ArrayList<AnnotationController>();
    boolean allParagraphAnnotations = false;

    // all annotations case and paragraph prefix selector
    if (keySet.isEmpty() || keySet.contains(AnnotationRegistry.PARAGRAPH_PREFIX)) {
      allParagraphAnnotations = true;
    }

    if (!allParagraphAnnotations) {
      paragraphKeySet.each(new Proc() {


        @Override
        public void apply(String key) {

          AnnotationController c = AnnotationRegistry.get(key);
          if (c.isParagraphStyle()) {
            controllers.add(c);
          }
        }

      });
    }

    final AnnotationController[] paragraphControllers = allParagraphAnnotations
        ? AnnotationRegistry.PARAGRAPH_STYLED_CONTROLLERS
        : controllers.toArray(new AnnotationController[] {});

    Paragraph.traverse(editor.getContent().getLocationMapper(), searchRange.getStart(),
        searchRange.getEnd(),
        new ContentElement.Action() {

          @Override
          public void execute(ContentElement e) {

            for (AnnotationController c : paragraphControllers) {
              String styleValue = c.isAppliedParagraphStyle(e);
              if (styleValue != null) {

                AnnotationValue anotationValue = AnnotationValueBuilder.buildForParagraphNode(
                    editor.getContent().getMutableDoc(), c.key, styleValue, e,
                    AnnotationValue.MATCH_IN);

                if (!result.containsKey(c.key)) {
                  result.setJso(c.key, JavaScriptObject.createArray());
                }

                JsUtils.addToArray(result.getJso(c.key), anotationValue);

              }
            }

          }

        });

    return result;

  }

  private final Type type;

  /** Annotation key */
  private String key;

  //
  // Paragraph annotations
  //

  private ParagraphBehaviour paragraphBehaviour;

  /** Paragraph styles */
  private Map<String, LineStyle> styles;

  /** Paragraph auto generated attributes */
  private AttributeGenerator attributeGenerator;

  //
  // Action-based paragraph annotations
  //

  private Map<String, ContentElement.Action> actions;
  private ContentElement.Action resetAction;

  private final ParagraphEventHandler paragraphEventHandler;
  private final TextEventHandler textEventHandler;

  /**
   * Constructor for text annotations.
   *
   * @param key
   * @param emptyValue
   */
  public AnnotationController(String key) {
    this.type = Type.TEXT;
    this.key = key;
    this.textEventHandler = new TextEventHandler(key);
    this.paragraphEventHandler = null;
  }

  /**
   * Constructor for paragraph annotations.
   *
   * @param key
   * @param styles
   * @param attributeGenerator
   */
  public AnnotationController(ParagraphBehaviour paragraphBehaviour, String key,
      Map<String, LineStyle> styles,
      AttributeGenerator attributeGenerator) {
    this.type = Type.PARAGRAPH_STYLE;
    this.paragraphBehaviour = paragraphBehaviour;
    this.key = key;
    this.styles = styles;
    this.attributeGenerator = attributeGenerator;
    this.textEventHandler = null;
    this.paragraphEventHandler = new ParagraphEventHandler(key, paragraphBehaviour);
    ParagraphEventHandler.register(key, this.paragraphEventHandler);
  }

  /**
   * Constructor action-based paragraph annotations.
   *
   * @param key
   * @param actions
   * @param resetAction
   */
  public AnnotationController(String key, Map<String, ContentElement.Action> actions,
      ContentElement.Action resetAction) {
    this.type = Type.PARAGRAPH_ACTION;
    this.actions = actions;
    this.resetAction = resetAction;
    this.textEventHandler = null;
    this.paragraphEventHandler = null;
  }

  public Type getType() {
    return type;
  }

  protected String getKey() {
    return key;
  }

  protected Map<String, LineStyle> getStyles() {
    return styles;
  }

  protected AttributeGenerator getAttributeGenerator() {
    return attributeGenerator;
  }

  public void setEventHandler(AnnotationEvent.Handler h) {

    if (h == null)
      return;

    if (type.equals(Type.TEXT)) {
      textEventHandler.setHandler(h);
    } else if (type.equals(Type.PARAGRAPH_STYLE)) {
      paragraphEventHandler.setHandler(h);
    }

  }

  public void unsetEventHandler() {
    if (type.equals(Type.TEXT)) {
      textEventHandler.setHandler(null);
    } else if (type.equals(Type.PARAGRAPH_STYLE)) {
      paragraphEventHandler.setHandler(null);
    }
  }

  public TextEventHandler getTextEventHanlder() {
    return textEventHandler;
  }

  /**
   * @return the style value, null if style is not applied
   */
  public String isAppliedParagraphStyle(ContentElement element) {
    if (styles == null) return null;

    String[] value = { null };

    styles.entrySet().forEach((Entry<String, LineStyle> e) -> {

      if (value[0] == null && e.getValue().isApplied(element)) {
        value[0] = e.getKey();
      }

    });

    return value[0];

  }

  public boolean isParagraphStyle() {
    return styles != null;
  }

}
