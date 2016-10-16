package x.swellrt.model;

import java.util.List;
import java.util.Map;

import x.swellrt.model.mutable.MutableCNode;

public class CPrimitive implements CNode, MutableCNode {
  
  public static final int TYPE_INT = 1;
  public static final int TYPE_DOUBLE = 2;
  public static final int TYPE_STRING = 3;
  public static final int TYPE_BOOL = 4;
  
  protected final int type;
  protected final int intValue;
  protected final double doubleValue;
  protected final String stringValue;
  protected final Boolean boolValue;
  
 
  public CPrimitive(int value) {
     type = TYPE_INT;
     intValue = value;
     doubleValue = Double.NaN;
     stringValue = null;
     boolValue = null;
  }

  public CPrimitive(double value) {
    type = TYPE_DOUBLE;
    intValue = Integer.MAX_VALUE;
    doubleValue = value;
    stringValue = null;
    boolValue = null;    
    
  }
  
  public CPrimitive(String value) {
    type = TYPE_STRING;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = value;    
    boolValue = null;    
  }
  
  public CPrimitive(boolean value) {
    type = TYPE_BOOL;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = null;  
    boolValue = value;    
  }
  
  public int getType() {
    return type;
  }

  @Override
  public Map<String, CNode> asMap() {
    throw new IllegalValueConversionException();
  }

  @Override
  public List<CNode> asList() {
    throw new IllegalValueConversionException();
  }

  @Override
  public String asString() {
    if (type == TYPE_STRING)
      return stringValue;
    
    throw new IllegalValueConversionException();
  }

  @Override
  public int asInteger() {
    if (type == TYPE_INT)
      return intValue;
    
    throw new IllegalValueConversionException();
  }

  @Override
  public double asDouble() {
    if (type == TYPE_DOUBLE)
      return doubleValue;
    
    throw new IllegalValueConversionException();
  }

  @Override
  public boolean asBoolean() {
    if (type == TYPE_BOOL)
      return boolValue;
    
    throw new IllegalValueConversionException();
  }
  
}
