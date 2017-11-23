package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;

/**
 * Utility methods to work with {@link SNode} trees that are platform dependent.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SNodeUtils {

  /** Retrieve a SNode referenced by a path inside a SNode tree */
  public SNode getNode(String path, SNode root) throws SException;

  /**
   * Retrieve a node referenced by a path inside a generic object. Use this
   * method to retrieve nodes inside JS trees. Using generic Object's parameters
   * to be cross-platform compatible.
   */
  public Object getNode(String path, Object root);

  /** Returns a JSON builder */
  public SViewBuilder jsonBuilder(SNode node);

  /**
   * Safely cast object to int, it might be a JavaScript object, undefined...
   * Return null if can't be casted.
   */
  public Integer castToInteger(Object value);

}
