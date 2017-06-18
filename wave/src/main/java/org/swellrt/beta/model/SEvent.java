package org.swellrt.beta.model;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Event")
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
  private final SNode node;

  /**
   *
   * @param type
   * @param targetNode
   * @param targetKey
   * @param node
   */
  @JsIgnore
  public SEvent(int type, SNode targetNode, String targetKey, SNode node) {
    super();
    this.type = type;
    this.target = targetNode;
    this.targetKey = targetKey;
    this.node = node;
  }

  @JsProperty
  public int getType() {
    return type;
  }

  @JsProperty
  public SNode getTarget() {
    return target;
  }

  @JsProperty
  public SNode getTargetNode() {
    return target;
  }

  @JsProperty
  public String getKey() {
    return targetKey;
  }

  @JsProperty
  public SNode getNode() {
    return node;
  }

  @JsProperty
  public SNode getValue() {
    return node;
  }

  @Override
  public String toString() {
    String s = "?";
    if (type == ADDED_VALUE) s = "added";
    if (type == REMOVED_VALUE) s = "removed";
    if (type == UPDATED_VALUE) s = "updated";
    return "SEvent(type=" + s + ") [" + targetKey + "," + node + "]";
  }
}
