package org.swellrt.beta.client.js.editor;

import org.swellrt.beta.client.js.Console;
import org.swellrt.beta.client.js.JsUtils;
import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsType;

/**
 * A text selection object wrapping and extending browser native selection
 * object. All types are provided as native Javascript objects.
 * <p>
 * {@link https://developer.mozilla.org/en-US/docs/Web/API/Selection }
 *
 * @author pablojan@gmail.com
 *
 */
@JsType(namespace = "swell.Editor", name = "Selection")
public class SSelection {

  protected static SSelection get(Range textRange) {

    FocusedPointRange<Node> nativeSelectionPoint = NativeSelectionUtil.get();

    try {

      SSelection s = new SSelection();

      s.anchorNode = nativeSelectionPoint.getAnchor().getCanonicalNode();
      s.anchorOffset = nativeSelectionPoint.getAnchor().getContainer().getNodeType() == Node.TEXT_NODE ? nativeSelectionPoint.getAnchor().getTextOffset() : 0;

      s.focusNode = nativeSelectionPoint.getFocus().getCanonicalNode();
      s.focusOffset = nativeSelectionPoint.getFocus().getContainer().getNodeType() == Node.TEXT_NODE ? nativeSelectionPoint.getFocus().getTextOffset() : 0;

      s.isCollapsed = nativeSelectionPoint.isCollapsed();

      s.range = JsUtils.rangeToNative(textRange);

    return s;

    } catch (RuntimeException e) {
      Console.log("Error getting browser selection: "+e.getMessage());
      return null;
    }


  }

  public static JavaScriptObject getRelativePosition(JavaScriptObject offsetPosition,
      Element relative) {
    Preconditions.checkNotNull(offsetPosition, "Offset position can't be null");
    Preconditions.checkNotNull(relative, "Relative element can't be null");
    OffsetPosition target = JsUtils.nativeToOffsetPosition(offsetPosition);
    return JsUtils.offsetPositionToNative(OffsetPosition.getRelativePosition(target, relative));
  }

  protected SSelection() {

  }


  public boolean isCollapsed;

  public Node anchorNode;

  public int anchorOffset;

  public Node focusNode;

  public int focusOffset;

  /** Range in the text document matching this selection */
  public JavaScriptObject range;

  public Element getElement() {
    return NativeSelectionUtil.getActiveElement();
  }

  public JavaScriptObject getFocusBound() {
    return JsUtils.intRangeToNative(NativeSelectionUtil.getFocusBounds());
  }

  public JavaScriptObject getSelectionPosition() {
    return JsUtils.offsetPositionToNative(NativeSelectionUtil.slowGetPosition());
  }

  public JavaScriptObject getAnchorPosition() {
    return JsUtils.offsetPositionToNative(NativeSelectionUtil.slowGetAnchorPosition());
  }

}
