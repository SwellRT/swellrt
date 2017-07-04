package org.swellrt.beta.client.js.editor;

import org.swellrt.beta.client.js.Console;
import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.IntRange;

import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell.Editor", name = "Selection")
public class SSelection {

  protected static SSelection get(Range textRange) {

    FocusedPointRange<Node> range = NativeSelectionUtil.get();

    try {

      SSelection s = new SSelection();

      s.anchorNode = range.getAnchor().getCanonicalNode();
      s.anchorOffset = range.getAnchor().getContainer().getNodeType() == Node.TEXT_NODE ? range.getAnchor().getTextOffset() : 0;

      s.focusNode = range.getFocus().getCanonicalNode();
      s.focusOffset = range.getFocus().getContainer().getNodeType() == Node.TEXT_NODE ? range.getFocus().getTextOffset() : 0;

      s.isCollapsed = range.isCollapsed();

      s.focusBound = NativeSelectionUtil.getFocusBounds();
      s.position = NativeSelectionUtil.slowGetPosition();
      s.anchorPosition = NativeSelectionUtil.slowGetAnchorPosition();

      s.range = textRange;

    return s;

    } catch (RuntimeException e) {
      Console.log("Error getting browser selection: "+e.getMessage());
      return null;
    }


  }

  protected SSelection() {

  }


  public boolean isCollapsed;

  public Node anchorNode;

  public int anchorOffset;

  public Node focusNode;

  public int focusOffset;

  public IntRange focusBound;

  public OffsetPosition position;

  public OffsetPosition anchorPosition;

  public Range range;
}
