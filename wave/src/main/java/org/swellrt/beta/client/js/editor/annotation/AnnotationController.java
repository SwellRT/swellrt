package org.swellrt.beta.client.js.editor.annotation;

import java.util.Map;
import java.util.function.Function;

import org.swellrt.beta.client.js.editor.SEditorException;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.LineStyle;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphBehaviour;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringSet;

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
    this.paragraphEventHandler.register();
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


}
