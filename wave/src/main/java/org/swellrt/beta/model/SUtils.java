package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.wave.mutable.SWaveNodeContainer;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.DocHelper;

import com.google.gwt.core.client.JavaScriptObject;

public class SUtils {

  public static SNode castToSNode(Object object, SNodeAccessControl token) throws IllegalCastException {

    if (object == null)
      throw new IllegalCastException("Error casting a null object");

    if (object instanceof String) {
      return new SPrimitive((String) object, token);
    } else if (object instanceof Integer) {
      return new SPrimitive((int) object, token);
    } else if (object instanceof Double) {
      return new SPrimitive((double) object, token);
    } else if (object instanceof Boolean) {
      return new SPrimitive((boolean) object, token);
    } else if (object instanceof SNode) {
      return (SNode) object;
    } else if (ModelFactory.instance.isJsonObject(object)) {
      return new SPrimitive(object, token);
    }

    throw new IllegalCastException("Error casting to primitive SNode");
  }


  public static SNode castToSNode(Object object) throws IllegalCastException {
    return castToSNode(object, new SNodeAccessControl());
  }

  /**
   * Introspect a generic object (java or javascript)
   * looking up a SNodeRemoteContainer.
   * <p>
   *
   * @param object
   * @return
   */
  public static SWaveNodeContainer asContainer(Object object) {
    if (object == null)
      return null;

    SWaveNodeContainer node = null;

    if (object instanceof JavaScriptObject) {
      JsoView jso = JsoView.as((JavaScriptObject) object);
      Object targetObject = jso.getObject("__target__");
      if (targetObject != null) {
        if (targetObject instanceof SWaveNodeContainer)
          node = (SWaveNodeContainer) targetObject;
      }
    } else if (object instanceof SWaveNodeContainer) {
      node = (SWaveNodeContainer) object;
    }

    return node;
  }


  public static <N, E extends N, T extends N> boolean isEmptyNode(ReadableDocument<N, E, T> doc, N node) {
    if (node == null)
      return true;

    int count = 0;
    N child = doc.getFirstChild(node);
    N first = child;
    while (child != null) {
      count++;
      child = doc.getNextSibling(child);
      if (count > 1) return false;
    }

    if (first == null) return true;

    return isEmptyNode(doc, first);

  }

  public static <N, E extends N, T extends N>  boolean isEmptyDocument(ReadableDocument<N, E, T> doc) {
    E body = DocHelper.getElementWithTagName(doc, "body");

    return isEmptyNode(doc, body);

  }
  /**
   * Clean any XML from the string
   * @param s
   * @return
   */
  public static String sanitizeString(String s) {
    return s.replace("<", "")
        .replace(">", "")
        .replace("/>", "");
  }

  public static void isValidMapKey(String key) throws SException {
    if (key != null && !key.contains("."))
      return;
    else
      throw new SException(SException.OPERATION_EXCEPTION, null, "Not valid key");
  }


}
