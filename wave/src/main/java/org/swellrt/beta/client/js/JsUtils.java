package org.swellrt.beta.client.js;

import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import com.google.gwt.core.client.JavaScriptObject;

public class JsUtils {

  public static native boolean isArray(JavaScriptObject o) /*-{
    return Array.isArray(o);
  }-*/;
  
  public static native boolean isString(JavaScriptObject o) /*-{
    return typeof o == 'string';
  }-*/;
  
  public static String[] stringSetToArray(ReadableStringSet s) {
    final String[] array = new String[s.countEntries()];
    
    
    s.each(new Proc() {

      int c = 0;
      
      @Override
      public void apply(String element) {
        array[c++] = element;
      } 
      
    });
    
    return array;
  }
  
  public static native void addToArray(JavaScriptObject array, Object value) /*-{
      array.push(value);
  }-*/;
}
