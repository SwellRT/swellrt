package org.swellrt.beta.model;

import org.swellrt.beta.model.remote.SNodeRemote;
import org.swellrt.beta.model.remote.SNodeRemoteContainer;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;


@SuppressWarnings("unused")
@JsType(namespace = "swellrt", name = "Primitive")
public class SPrimitive implements SNode, SNodeRemote {

  private static final String SEPARATOR = ":";
  private static final String STRING_TYPE_PREFIX  = "s";
  private static final String BOOLEAN_TYPE_PREFIX  = "b";
  private static final String INTEGER_TYPE_PREFIX  = "i";
  private static final String DOUBLE_TYPE_PREFIX  = "d";
  private static final String JSO_TYPE_PREFIX  = "js";
  
  private static final int TYPE_INT = 1;
  private static final int TYPE_DOUBLE = 2;
  private static final int TYPE_STRING = 3;
  private static final int TYPE_BOOL = 4;
  private static final int TYPE_JSO = 5;

  private final int type;
  private final int intValue;
  private final double doubleValue;
  private final String stringValue;
  private final Boolean boolValue;
  private final JavaScriptObject jsoValue;

  /** 
   * the key associated with this value in its parent container
   * if it is a map.
   */
  private String nameKey = null;
  
  /** the container node */
  private SNode container = null;
  
  /**
   * Deserialize a SPrimitive
   * @param s the serialized representation of the primitive value. 
   * @return the primitive value or null if is not a valid serialized string
   */
  public static SPrimitive deserialize(String s) {
    Preconditions.checkArgument(s != null && !s.isEmpty(), "Null or empty string");
    
    if (s.startsWith(STRING_TYPE_PREFIX+SEPARATOR)) {
      return new SPrimitive(s.substring(2));
    }
    
    if (s.startsWith(INTEGER_TYPE_PREFIX+SEPARATOR)) {
      try {
       return new SPrimitive(Integer.parseInt(s.substring(2)));
      } catch (NumberFormatException e) {
        // Oops
        return null;
      }
    }

    if (s.startsWith(DOUBLE_TYPE_PREFIX+SEPARATOR)) {
      try {
       return new SPrimitive(Double.parseDouble(s.substring(2)));
      } catch (NumberFormatException e) {
        // Oops
        return null;
      }
    }
    
    if (s.startsWith(BOOLEAN_TYPE_PREFIX+SEPARATOR)) {
       return new SPrimitive(Boolean.parseBoolean(s.substring(2)));
    }
    
    if (s.startsWith(JSO_TYPE_PREFIX+SEPARATOR)) {
      return new SPrimitive(JsonUtils.<JavaScriptObject>safeEval(s.substring(3)));
    }
    
    return null;
  }
  
  
  public String serialize() {
    
    if (type == TYPE_STRING)
      return STRING_TYPE_PREFIX+SEPARATOR+stringValue;
    
    if (type == TYPE_BOOL)
      return BOOLEAN_TYPE_PREFIX+SEPARATOR+Boolean.toString(boolValue);
    
    if (type == TYPE_INT)
      return INTEGER_TYPE_PREFIX+SEPARATOR+Integer.toString(intValue);
    
    if (type == TYPE_DOUBLE)
      return DOUBLE_TYPE_PREFIX+SEPARATOR+Double.toString(doubleValue);
    
    if (type == TYPE_JSO)
      return JSO_TYPE_PREFIX+SEPARATOR+JsonUtils.stringify(jsoValue);
    
    return null;    
  }
  
  
  
  @JsIgnore
  public SPrimitive(int value) {
    type = TYPE_INT;
    intValue = value;
    doubleValue = Double.NaN;
    stringValue = null;
    boolValue = null;
    jsoValue = null;
  }

  @JsIgnore
  public SPrimitive(double value) {
    type = TYPE_DOUBLE;
    intValue = Integer.MAX_VALUE;
    doubleValue = value;
    stringValue = null;
    boolValue = null;
    jsoValue = null;

  }

  @JsIgnore
  public SPrimitive(String value) {
    type = TYPE_STRING;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = value;
    boolValue = null;
    jsoValue = null;
  }

  @JsIgnore
  public SPrimitive(boolean value) {
    type = TYPE_BOOL;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = null;
    boolValue = value;
    jsoValue = null;
  }

  @JsIgnore
  public SPrimitive(JavaScriptObject value) {
    type = TYPE_JSO;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = null;
    boolValue = null;
    jsoValue = value;
  }

  @JsProperty
  public int getType() {
    return type;
  }

  public Object get() {

    if (type == TYPE_STRING)
      return stringValue;

    if (type == TYPE_INT)
      return intValue;

    if (type == TYPE_DOUBLE)
      return doubleValue;

    if (type == TYPE_BOOL)
      return boolValue;

    if (type == TYPE_JSO)
      return jsoValue;

    return null;
  }
  
  @Override
  public String toString() {
    return "Primitive Value ["+ serialize()+"]";    
  }

  @JsIgnore
  public void setNameKey(String nameKey) {
    this.nameKey = nameKey;
  }
  
  @JsIgnore
  public String getNameKey() {
    return this.nameKey;
  }

  @JsIgnore
  public SNode getContainer() {
    return container;
  }

  @JsIgnore
  public void setContainer(SNode container) {
    this.container = container;
  }
  
}
