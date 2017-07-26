package org.swellrt.beta.client.js.editor.annotation;

import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsIgnore;

/**
 * Build annotation values
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AnnotationValueBuilder {


  private static Element getContainerLineElement(Point<ContentNode> point) {

    ContentNode n = point.getContainer();
    Element lineElement = null;
    boolean stop = false;
    while (!stop) {

      if (n == null) {
        stop = true;
      } else if (n.isTextNode()) {
        n = n.getParentElement();
      } else if (n.isElement()) {
        if (n.asElement().getTagName().equals(LineContainers.PARAGRAPH_FULL_TAGNAME)) {
          lineElement = n.asElement().getImplNodelet();
          stop = true;
        } else {
          n = n.getParentElement();
        }
      }

    }

    return lineElement;
  }



  public static Node getMiddleNode(CMutableDocument doc, Range range) {

    int location = range.getStart() + ((range.getEnd() - range.getStart()) / 2) + 1;

    Point<ContentNode> n = doc.locate(location);

    return n.getContainer().getImplNodeletRightwards();
  }

  /**
   * Use this method from annotation search. See {@link AnnotationRegistry}
   */
  @JsIgnore
  public static AnnotationValue buildWithRange(CMutableDocument doc, String key, Object value,
      Range range,
      int searchMatch) {

    Preconditions.checkArgument(range != null, "Range can't be null");

    AnnotationValue av = new AnnotationValue();

    av.key = key;
    av.value = value;
    av.range = range;
    av.searchMatch = searchMatch;

    Point<ContentNode> point = doc.locate(range.getStart());

    av.text = DocHelper.getText(doc, range.getStart(), range.getEnd());
    av.line = getContainerLineElement(point);
    av.node = getMiddleNode(doc, range);

    av.data = point.getContainer();


    return av;
  }

  /**
   * Use this method from annotation search. See {@link AnnotationRegistry}
   */
  @JsIgnore
  public static AnnotationValue buildForParagraphNode(CMutableDocument doc, String key,
      Object value, ContentElement element, int searchMatch) {

    // element is the <line/> but we want the <l:p> node
    ContentNode line = element.getNextSibling();

    int start = element.getContext().locationMapper().getLocation(line);
    ContentNode endLine = line.getNextSibling();
    int end = -1;
    if (endLine != null) {
      end = element.getContext().locationMapper().getLocation(endLine);
    } else {
      Point<ContentNode> endPoint = Point.end(element.getContext().document().getDocumentElement());
      end = element.getContext().locationMapper().getLocation(endPoint);
    }

    AnnotationValue av = new AnnotationValue();

    av.key = key;
    av.value = value;
    av.range = Range.create(start, end);
    av.searchMatch = searchMatch;

    av.node = element.getImplNodeletRightwards();
    av.line = av.node;
    av.text = DocHelper.getText(doc, av.range.getStart(), av.range.getEnd());

    // if (av.node.getNodeType() == Node.ELEMENT_NODE) {
    // av.text = ((Element) av.node).getInnerText();
    // } else if (av.node.getNodeType() == Node.TEXT_NODE) {
    // av.text = ((Text) av.node).getData();
    // } else {
    //
    // }

    av.data = element;

    return av;
  }

  private static RangedAnnotation<String> getAnnotationFromNode(String key, String value,
      ContentElement node) {

    int location = node.getLocationMapper().getLocation(node);
    if (location + 1 > node.getMutableDoc().size())
      location--;
    Iterable<RangedAnnotation<String>> it = node.getMutableDoc().rangedAnnotations(location,
        location + 1, CollectionUtils.newStringSet(key));
    for (RangedAnnotation<String> ra : it) {
      if (ra.key() != null && ra.key().equals(key) && ra.value() != null
          && ra.value().equals(value))
        return ra;
    }

    return null;
  }

  private static RangedAnnotation<Object> getLocalAnnotationFromNode(String key, Object value,
      ContentElement node) {

    int location = node.getLocationMapper().getLocation(node);
    if (location + 1 > node.getMutableDoc().size())
      location--;
    Iterable<RangedAnnotation<Object>> it = node.getContext().localAnnotations().rangedAnnotations(
        location,
        location + 1, CollectionUtils.newStringSet(key));
    for (RangedAnnotation<Object> ra : it) {
      if (ra.key().equals(key) && ra.value().equals(value))
        return ra;
    }

    return null;
  }

  /**
   * Use this method from event handlers. See {@link TextAnnotation}
   */
  @JsIgnore
  public static AnnotationValue buildWithNode(String key, Object value, ContentElement node) {

    Range range = null;

    if (Annotations.isLocal(key)) {
      RangedAnnotation<Object> ra = getLocalAnnotationFromNode(key, value.toString(), node);
      if (ra != null)
        range = Range.create(ra.start(), ra.end());
    } else {
      RangedAnnotation<String> ra = getAnnotationFromNode(key, value.toString(), node);
      if (ra != null)
        range = Range.create(ra.start(), ra.end());
    }



    AnnotationValue av = new AnnotationValue();

    av.key = key;
    av.value = value;
    av.range = range != null ? range : Range.ALL;
    av.searchMatch = -1;

    if (node.isImplAttached()) {
      Point<ContentNode> point = Point.before(node.getContext().document(), node);
      av.line = getContainerLineElement(point);
      av.node = node.getImplNodeletRightwards();
    } else {
      av.node = node.getImplNodelet();
    }


    av.data = node;

    return av;

  }



  public static int getRangeMatch(Range selectionRange, Range annotationRange) {
    boolean in = (selectionRange.getStart() >= annotationRange.getStart()
        && selectionRange.getEnd() <= annotationRange.getEnd());
    return in ? AnnotationValue.MATCH_IN : AnnotationValue.MATCH_OUT;
  }

}
