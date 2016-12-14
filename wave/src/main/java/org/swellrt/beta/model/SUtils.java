package org.swellrt.beta.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SUtils {

  public static SNode castToPrimitive(Object object) throws IllegalCastException {
    
    if (object == null)
      throw new IllegalCastException("Error casting a null object");
    
    if (object instanceof String) {
      return new SPrimitive((String) object);
    } else if (object instanceof Integer) {
      return new SPrimitive((Integer) object);      
    } else if (object instanceof Double) {
      return new SPrimitive((Double) object); 
    } else if (object instanceof Boolean) {
      return new SPrimitive((Boolean) object); 
    } else if (object instanceof SNode) {
      return (SNode) object;
    } else if (object instanceof JavaScriptObject) {
      return new SPrimitive((JavaScriptObject) object);
    }
  
    throw new IllegalCastException("Error casting to primitive SNode");
  }

}
