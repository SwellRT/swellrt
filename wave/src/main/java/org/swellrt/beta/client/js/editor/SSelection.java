package org.swellrt.beta.client.js.editor;

import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.util.IntRange;

import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt.Editor", name = "Selection")
public class SSelection {

  protected static SSelection get() {
    
    SSelection s = new SSelection();
    
    FocusedPointRange<Node> range = NativeSelectionUtil.get();
    
    s.anchorNode = range.getAnchor().getCanonicalNode();
    s.anchorOffset = range.getAnchor().getContainer().getNodeType() == Node.TEXT_NODE ? range.getAnchor().getTextOffset() : 0;
    
    s.focusNode = range.getFocus().getCanonicalNode();
    s.focusOffset = range.getFocus().getContainer().getNodeType() == Node.TEXT_NODE ? range.getFocus().getTextOffset() : 0;
    
    s.isCollapsed = range.isCollapsed();
        
    s.focusBound = NativeSelectionUtil.getFocusBounds();    
    s.position = NativeSelectionUtil.slowGetPosition();    
    s.anchorPosition = NativeSelectionUtil.slowGetAnchorPosition();
    
    return s;
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
}
