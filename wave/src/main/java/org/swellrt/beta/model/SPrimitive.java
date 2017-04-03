package org.swellrt.beta.model;

import org.swellrt.beta.model.remote.SNodeRemote;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;


@JsType(namespace = "swellrt", name = "Primitive")
public class SPrimitive extends SNodeRemote {

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
  
  private final SNodeAccessControl accessControl; 

  /** 
   * the key associated with this value in its parent container
   * if it is a map.
   */
  private String nameKey = null;
  
  @JsIgnore
  public static String asString(SNode node) {
    try{
      if (node != null && node instanceof SPrimitive) {
        return (String) ((SPrimitive) node).get();
      }
    } catch (ClassCastException e)
    {
      
    }
    return null;
  }
  
  @JsIgnore
  public static Double asDouble(SNode node) {
    try{
      if (node != null && node instanceof SPrimitive) {
        return (double) ((SPrimitive) node).get();
      }
    } catch (ClassCastException e)
    {
      
    }
    return null;
  }
  
  @JsIgnore
  public static Integer asInt(SNode node) {
    try{
      if (node != null && node instanceof SPrimitive) {
        return (int) ((SPrimitive) node).get();
      }
    } catch (ClassCastException e)
    {
      
    }
    return null;
  }
  
  @JsIgnore
  public static Boolean asBoolean(SNode node) {
    try{
      if (node != null && node instanceof SPrimitive) {
        return (boolean) ((SPrimitive) node).get();
      }
    } catch (ClassCastException e)
    {
      
    }
    return null;
  }

  
  
  /**
   * Deserialize a SPrimitive value
   * <p>
   * @param s the serialized representation of the primitive value. 
   * @return the primitive value or null if is not a valid serialized string
   */
  public static SPrimitive deserialize(String s) {
    Preconditions.checkArgument(s != null && !s.isEmpty(), "Null or empty string");
    
    SNodeAccessControl acToken = null;
    if (SNodeAccessControl.isToken(s)) {
      int firstSepIndex = s.indexOf(SEPARATOR);
      acToken = SNodeAccessControl.deserialize(s.substring(0, firstSepIndex));     
      s = s.substring(firstSepIndex+1);
    } else {
      acToken = new SNodeAccessControl();
    }
    
    
    if (s.startsWith(STRING_TYPE_PREFIX+SEPARATOR)) {
      return new SPrimitive(s.substring(2), acToken);
    }
    
    if (s.startsWith(INTEGER_TYPE_PREFIX+SEPARATOR)) {
      try {
       return new SPrimitive(Integer.parseInt(s.substring(2)), acToken);
      } catch (NumberFormatException e) {
        // Oops
        return null;
      }
    }

    if (s.startsWith(DOUBLE_TYPE_PREFIX+SEPARATOR)) {
      try {
       return new SPrimitive(Double.parseDouble(s.substring(2)), acToken);
      } catch (NumberFormatException e) {
        // Oops
        return null;
      }
    }
    
    if (s.startsWith(BOOLEAN_TYPE_PREFIX+SEPARATOR)) {
       return new SPrimitive(Boolean.parseBoolean(s.substring(2)), acToken);
    }
    
    if (s.startsWith(JSO_TYPE_PREFIX+SEPARATOR)) {
      return new SPrimitive(JsonUtils.<JavaScriptObject>safeEval(s.substring(3)), acToken);
    }
    
    return null;
  }
  
  
  public String serialize() {
    
    String token = accessControl.serialize();
    if (!token.isEmpty())
      token += SEPARATOR;
      
    
    if (type == TYPE_STRING)
      return token + STRING_TYPE_PREFIX+SEPARATOR+stringValue;
    
    if (type == TYPE_BOOL)
      return token + BOOLEAN_TYPE_PREFIX+SEPARATOR+Boolean.toString(boolValue);
    
    if (type == TYPE_INT)
      return token + INTEGER_TYPE_PREFIX+SEPARATOR+Integer.toString(intValue);
    
    if (type == TYPE_DOUBLE)
      return token + DOUBLE_TYPE_PREFIX+SEPARATOR+Double.toString(doubleValue);
    
    if (type == TYPE_JSO)
      return token + JSO_TYPE_PREFIX+SEPARATOR+JsonUtils.stringify(jsoValue);
    
    return null;    
  }

  
  @JsIgnore
  public SPrimitive(int value, SNodeAccessControl token) {
    type = TYPE_INT;
    intValue = value;
    doubleValue = Double.NaN;
    stringValue = null;
    boolValue = null;
    jsoValue = null;
    accessControl = token;
  }

  @JsIgnore
  public SPrimitive(double value, SNodeAccessControl token) {
    type = TYPE_DOUBLE;
    intValue = Integer.MAX_VALUE;
    doubleValue = value;
    stringValue = null;
    boolValue = null;
    jsoValue = null;
    accessControl = token;    
  }

  @JsIgnore
  public SPrimitive(String value, SNodeAccessControl token) {
    type = TYPE_STRING;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = value;
    boolValue = null;
    jsoValue = null;
    accessControl = token;    
  }

  @JsIgnore
  public SPrimitive(boolean value, SNodeAccessControl token) {
    type = TYPE_BOOL;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = null;
    boolValue = value;
    jsoValue = null;
    accessControl = token;    
  }

  @JsIgnore
  public SPrimitive(JavaScriptObject value, SNodeAccessControl token) {
    type = TYPE_JSO;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = null;
    boolValue = null;
    jsoValue = value;
    accessControl = token;    
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

  
  
  /**
   * Check if this value can be written by a participant
   * @param node
   * @param participantId
   * @return
   */
  @JsIgnore
  public boolean canWrite(ParticipantId participantId) {
    return accessControl.canWrite(participantId);
  }
  
  /**
   * Check if this value can be read by a participant
   * @param node
   * @param participantId
   * @return
   */
  @JsIgnore
  public boolean canRead(ParticipantId participantId) {
    return accessControl.canRead(participantId);
  }
  
  protected SNodeAccessControl getNodeAccessControl() {
    return accessControl;
  }
}
