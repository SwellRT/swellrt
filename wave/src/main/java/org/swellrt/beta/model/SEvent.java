package org.swellrt.beta.model;

public class SEvent {
  
  public static final int ADDED_VALUE = 11;
  public static final int REMOVED_VALUE = 12;
  public static final int UPDATED_VALUE = 13;
  
  private final int type;
  
  /** The map or list containing the value */
  private final SNode target;
  
  /** The key of the value, if target is map */
  private final String targetKey;
  
  /** The actual value, added, removed or updated */
  private final SNode value;

  public SEvent(int type, SNode target, String targetKey, SNode value) {
    super();
    this.type = type;
    this.target = target;
    this.targetKey = targetKey;
    this.value = value;
  }

  public int getType() {
    return type;
  }

  public SNode getTarget() {
    return target;
  }

  public String getTargetKey() {
    return targetKey;
  }

  public SNode getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return "SEvent ["+targetKey+","+value+"]";
  }
}
