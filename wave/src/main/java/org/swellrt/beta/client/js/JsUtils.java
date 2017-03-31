package org.swellrt.beta.client.js;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringSet;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class JsUtils {

  @Deprecated
  public static native boolean isArray(JavaScriptObject o) /*-{
    return Array.isArray(o);
  }-*/;
  
  @Deprecated
  public static native boolean isString(JavaScriptObject o) /*-{
    return typeof o == 'string';
  }-*/;
  
  public static native JsArrayString asArray(JavaScriptObject o) /*-{
    if (Array.isArray(o))
      return o;
    
    return null;
  }-*/;
  
  
  public static native String asString(JavaScriptObject o) /*-{
    if (typeof o == 'string')
      return o;
    
    return null;
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

  
  
  /**
   * Transform a Javascript object (array or string) to
   * a StringSet.
   * 
   * @param obj an array or string
   * @return
   */
  public static StringSet toStringSet(JavaScriptObject obj) throws IllegalStateException {
    
    StringSet set = CollectionUtils.createStringSet();

    // from array
    JsArrayString array = asArray(obj);
    if (array != null) {
      for (int i = 0; i < array.length(); i++)
        set.add(array.get(i));
    }
  
    
    // from string
    String s = asString(obj);
    if (s!=null)
      set.add(s);
        
    return set;
  }
}
