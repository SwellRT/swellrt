package x.swellrt.model.mutable;

import java.util.List;
import java.util.Map;

import x.swellrt.model.CNode;
import x.swellrt.model.IllegalValueConversionException;

public class MutableCPrimitive implements MutableCNode, CNode {
  
  protected static final int TYPE_INT = 1;
  protected static final int TYPE_DOUBLE = 2;
  protected static final int TYPE_STRING = 3;
  protected static final int TYPE_BOOL = 4;
  
  protected final int type;
  protected final int intValue;
  protected final double doubleValue;
  protected final String stringValue;
  protected final Boolean boolValue;
  
 
  protected MutableCPrimitive(int value) {
     type = TYPE_INT;
     intValue = value;
     doubleValue = Double.NaN;
     stringValue = null;
     boolValue = null;
  }

  protected MutableCPrimitive(double value) {
    type = TYPE_DOUBLE;
    intValue = Integer.MAX_VALUE;
    doubleValue = value;
    stringValue = null;
    boolValue = null;    
    
  }
  
  protected MutableCPrimitive(String value) {
    type = TYPE_STRING;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = value;    
    boolValue = null;    
  }
  
  protected MutableCPrimitive(boolean value) {
    type = TYPE_BOOL;
    intValue = Integer.MAX_VALUE;
    doubleValue = Double.NaN;
    stringValue = null;  
    boolValue = value;    
  }
  
  protected int getType() {
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
