package org.swellrt.client.editor;

import com.google.gwt.core.client.JavaScriptObject;


public interface TextEditorListener {

  public void onSelectionChange(int start, int end, JavaScriptObject annotations);


}
