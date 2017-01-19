package org.swellrt.beta.model;

import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Helper")

public class SHelper {
  
  public static SNode createWithACL(Object object, @JsOptional Boolean isReadOnly,  @JsOptional String[] writers, @JsOptional String[] readers) throws IllegalCastException {    
    SNodeAccessControl.Builder acBuilder = new SNodeAccessControl.Builder(); 
    acBuilder.setReadOnly(isReadOnly);
    acBuilder.read(readers);
    acBuilder.write(writers);
    return SUtils.castToSNode(object, acBuilder.build());
  }
  
  public static SNode createReadOnly(Object object) throws IllegalCastException {
    return SUtils.castToSNode(object, new SNodeAccessControl(true));
  }
}
