package org.swellrt.beta.client.js.editor.annotation;

import java.util.function.Consumer;

import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.StringSet;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Event;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 *
 *
 * @author pablojan@gmail.com
 *
 */
@JsType(namespace = "swell", name = "Annotation")
public class AnnotationInstance {

  public static final int EVENT_ADDED = 1;
  public static final int EVENT_MUTATED = 2;
  public static final int EVENT_REMOVED = 3;
  public static final int EVENT_MOUSE = 4;

  @JsFunction
  public interface Handler {

    public boolean exec(int type, AnnotationInstance annotation, @JsOptional Event event);
  }

  /** the selection range is equals to annotation range or inside it */
  public static final int MATCH_IN = 0;
  /**
   * the selection range is partially out of the annotation range or selection
   * spans more content beyond annotation
   */
  public static final int MATCH_OUT = 1;

  public static int getRangeMatch(Range selectionRange, Range annotationRange) {
    boolean in = (selectionRange.getStart() >= annotationRange.getStart()
        && selectionRange.getEnd() <= annotationRange.getEnd());
    return in ? AnnotationInstance.MATCH_IN : AnnotationInstance.MATCH_OUT;
  }

  private final Annotation annotation;

  // public to Js */
  public final String name;
  public String value;
  public Range range;
  public int matchType;
  public String text;

  private final LocationMapper<ContentNode> mapper;
  private final CMutableDocument doc;
  private final MutableAnnotationSet<Object> localAnnotations;
  private final CaretAnnotations caret;

  private final ContentElement node; // optional

  protected static Node lookupNodelet(ContentNode n) {
    if (n != null) {
      Node rightwardsNodelet = n.getImplNodeletRightwards();
      if (rightwardsNodelet != null) {
        return rightwardsNodelet;
      }
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  protected static Range getAnnotationRange(ContentElement node, String name) {

    int start = node.getMutableDoc().getLocation(node);
    int end = node.getMutableDoc().getLocation(node.getNextSibling());

    StringSet keys = JsoStringSet.create();
    keys.add(name);

    Range[] result = new Range[1];

    Consumer<RangedAnnotation> rangeMatcher = new Consumer<RangedAnnotation>() {

      @Override
      public void accept(RangedAnnotation r) {
        if (r.value() != null && r.key().equals(name)) {
          result[0] = new Range(r.start(), r.end());
        }
      }

    };

    if (Annotation.isLocal(name)) {
      node.getContext().localAnnotations().rangedAnnotations(start, end, keys)
          .forEach(rangeMatcher);
    } else {
      node.getMutableDoc().rangedAnnotations(start, end, keys).forEach(rangeMatcher);
    }

    return result[0];
  }

  /**
   * Use this method from annotation search. See {@link AnnotationRegistry}
   */
  @JsIgnore
  public static AnnotationInstance create(ContentDocument doc, String name, String value,
      Range range, int matchType) {

    return AnnotationInstance.create(name, value, doc.getMutableDoc(), doc.getLocationMapper(),
        doc.getLocalAnnotations(), doc.getContext().editing().editorContext().getCaretAnnotations(),
        range, matchType, null);

  }

  @JsIgnore
  public static AnnotationInstance create(String name, String value, Range range,
      ContentElement node, int matchType) {

    return AnnotationInstance.create(name, value, node.getMutableDoc(), node.getLocationMapper(),
        node.getContext().localAnnotations(),
        node.getContext().editing().editorContext().getCaretAnnotations(), range, matchType, node);
  }

  /**
   * Use this method from event handlers. See {@link TextAnnotation}
   */
  @JsIgnore
  public static AnnotationInstance create(String name, String value, ContentElement node) {
    Range range = getAnnotationRange(node, name);

    return AnnotationInstance.create(name, value, node.getMutableDoc(), node.getLocationMapper(),
        node.getContext().localAnnotations(),
        node.getContext().editing().editorContext().getCaretAnnotations(), range, MATCH_IN, node);
  }

  @JsIgnore
  public static AnnotationInstance create(String name, String value, CMutableDocument doc,
      LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations,
      CaretAnnotations caret, Range range, int matchType, ContentElement node) {
    return new AnnotationInstance(name, value, doc, mapper, localAnnotations, caret, range,
        matchType, node);
  }

  protected AnnotationInstance(String name, String value, CMutableDocument doc,
      LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations,
      CaretAnnotations caret, Range range, int matchType, ContentElement node) {
    super();
    this.annotation = AnnotationRegistry.get(name);
    this.name = name;
    this.value = value;
    this.range = range != null ? Range.create(range.getStart(), range.getEnd()) : null; // clone
    this.matchType = matchType;
    this.mapper = this.doc = doc;
    try {
      // sometimes range's content change faster than this call,
      // prevent exception
      this.text = range != null ? DocHelper.getText(doc, range.getStart(), range.getEnd()) : "";
    } catch (Exception e) {
      this.text = "";
    }
    this.localAnnotations = localAnnotations;
    this.caret = caret;
    this.node = node;
  }

  @JsProperty
  public Element getLine() {
    if (range != null) {
      Point<ContentNode> point = doc.locate(range.getStart() + 1);
      ContentElement lineNode = LineContainers.getRelatedLineElement(doc, point);
      return lookupNodelet(lineNode).getParentElement();
    } else if (node != null) {
      ContentElement lineNode = LineContainers.getRelatedLineElement(doc,
          doc.locate(doc.getLocation(node)));
      return lookupNodelet(lineNode).getParentElement();
    }

    return null;
  }

  @JsProperty
  public Node getNode() {
    Node e = lookupNodelet(node);

    if (e == null && range != null) {
      Point<ContentNode> point = doc.locate(range.getStart() + 1);
      e = point.getContainer().getParentElement().getImplNodelet();
    }

    return e;
  }

  public void update(String value) {
    if (range != null) {
      annotation.update(doc, mapper, localAnnotations, caret, range, value);
      this.value = value;
    }
  }

  public void mutate(String text) {

    if (range != null) {
      Range mutatedRange = annotation.mutate(doc, mapper, localAnnotations, caret, range, text,
          value);
      this.text = text;
      this.range = mutatedRange;
    }
  }

  public void clear() {
    if (range != null) {
      annotation.reset(doc, mapper, localAnnotations, caret, range);
      this.value = null;
      this.text = null;
      this.range = null;
    }
  }

}
