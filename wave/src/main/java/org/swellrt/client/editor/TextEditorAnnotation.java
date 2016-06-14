package org.swellrt.client.editor;

import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.LineStyle;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.HashMap;
import java.util.Map;

public class TextEditorAnnotation {

  public interface LineStyleFactory {
   public Map<String, Paragraph.LineStyle> getStyles();
  }


  public static final String PARAGRAPH_ANNOTATION_PREFIX = "paragraph";

  public enum ParagraphAnnotation {

    LIST_STYLE_TYPE(PARAGRAPH_ANNOTATION_PREFIX + "/" + "listStyleType", "unordered",
       new LineStyleFactory() {
      @Override
      public Map<String, LineStyle> getStyles() {
        Map<String, LineStyle> m = new HashMap<String, LineStyle>();
        m.put("decimal", Paragraph.listStyle(Paragraph.LIST_STYLE_DECIMAL));
        m.put("unordered", Paragraph.listStyle(null));
        return m;
      }

    }),

    TEXT_ALIGN(PARAGRAPH_ANNOTATION_PREFIX + "/" + "textAlign", "left", new LineStyleFactory() {

      @Override
      public Map<String, LineStyle> getStyles() {
        Map<String, LineStyle> m = new HashMap<String, LineStyle>();
        m.put("left", Paragraph.Alignment.LEFT);
        m.put("center", Paragraph.Alignment.CENTER);
        m.put("right", Paragraph.Alignment.RIGHT);
        m.put("justify", Paragraph.Alignment.JUSTIFY);
        return m;
      }

    }),

    HEADER(PARAGRAPH_ANNOTATION_PREFIX + "/" + "header", "h5", new LineStyleFactory() {

      @Override
      public Map<String, LineStyle> getStyles() {
        Map<String, LineStyle> m = new HashMap<String, LineStyle>();
        m.put("h1", Paragraph.regularStyle("h1"));
        m.put("h2", Paragraph.regularStyle("h2"));
        m.put("h3", Paragraph.regularStyle("h3"));
        m.put("h4", Paragraph.regularStyle("h4"));
        m.put("h5", Paragraph.regularStyle("h5"));
        return m;
      }

    });


    public static ParagraphAnnotation fromString(String str) {
      if (str.equals(PARAGRAPH_ANNOTATION_PREFIX + "/" + "listStyleType")) return LIST_STYLE_TYPE;
      if (str.equals(PARAGRAPH_ANNOTATION_PREFIX + "/" + "textAlign")) return TEXT_ALIGN;
      if (str.equals(PARAGRAPH_ANNOTATION_PREFIX + "/" + "header")) return HEADER;
      return null;
    }

    String name;
    String defaultValue;
    Map<String, LineStyle> values;

    private ParagraphAnnotation(String name, String defaultValue, LineStyleFactory valuesFactory) {
      this.name = name;
      this.values = valuesFactory.getStyles();
      this.defaultValue = defaultValue;
    }

    public LineStyle getLineStyleForValue(String value) {
      if (value == null || value.isEmpty()) return values.get(defaultValue);
      return values.get(value);
    }

    @Override
    public String toString() {
      return this.name;
    }

  }


  /**
   * List of annotations that applies at character level
   */
  public static final ReadableStringSet CARET_ANNOTATIONS = CollectionUtils.newStringSet(
      AnnotationConstants.STYLE_BG_COLOR,
      AnnotationConstants.STYLE_COLOR,
      AnnotationConstants.STYLE_FONT_FAMILY,
      AnnotationConstants.STYLE_FONT_SIZE,
      AnnotationConstants.STYLE_FONT_STYLE,
      AnnotationConstants.STYLE_FONT_WEIGHT,
      AnnotationConstants.STYLE_TEXT_DECORATION,
      AnnotationConstants.STYLE_VERTICAL_ALIGN,
      AnnotationConstants.LINK_AUTO,
      AnnotationConstants.LINK_MANUAL
    );

  public static final ReadableStringSet PARAGRAPH_ANNOTATIONS = CollectionUtils.newStringSet(
          ParagraphAnnotation.LIST_STYLE_TYPE.toString(),
          ParagraphAnnotation.TEXT_ALIGN.toString(),
          ParagraphAnnotation.HEADER.toString());


  public static boolean isParagraphAnnotation(String annotationName) {
    return annotationName != null && annotationName.startsWith(PARAGRAPH_ANNOTATION_PREFIX);
  }

}
