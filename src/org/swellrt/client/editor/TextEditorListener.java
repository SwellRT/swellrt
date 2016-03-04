package org.swellrt.client.editor;

import org.waveprotocol.wave.model.util.ReadableStringMap;

public interface TextEditorListener {

  public void onSelectionChange(int start, int end, ReadableStringMap<String> annotations);


}
