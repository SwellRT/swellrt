package org.swellrt.beta.client.js.editor;

import org.swellrt.beta.common.SException;

@SuppressWarnings("serial")
public class SEditorException extends SException {

  public static final int GENERIC_EDITOR_EXCEPTION = 200;
  public static final int UNKNOWN_ANNOTATION = 201;
  
  
  public SEditorException(int code, Throwable parent, String message) {
    super(code, parent, message);
  }

  public SEditorException(Throwable parent, String message) {
    super(GENERIC_EDITOR_EXCEPTION, parent, message);
  }
  
  
  public SEditorException(String message) {
    super(GENERIC_EDITOR_EXCEPTION, null, message);
  }
  
  public SEditorException(int code, String message) {
    super(code, null, message);
  }

}
