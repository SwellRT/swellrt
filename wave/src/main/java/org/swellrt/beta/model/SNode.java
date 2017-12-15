package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.util.Preconditions;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Node")
public interface SNode {


  /** Set a property value in an object */
  public static void set(SNode root, String path, Object value) {

    Preconditions.checkArgument(path != null && !path.isEmpty(), "Path is empty or null");
    Preconditions.checkArgument(value != null, "Can't set a null value");

    String property = null;
    int separatorPos = path.lastIndexOf(".");
    if (separatorPos != -1) {
      property = path.substring(separatorPos + 1);
      path = path.substring(0, separatorPos);
    } else {
      property = path;
      path = "";
    }

    try {
      SNodeLocator.Location location = SNodeLocator.locate(root, new PathNavigator(path));

      if (location.isJsonObject() || location.isJsonProperty()) {
        throw new IllegalStateException("Node can't be mutated");
      }

      if (location.node instanceof SMap) {

        location.node.asMap().put(property, value);

      } else if (location.node instanceof SList) {

        try {

          int index = Integer.valueOf(property);
          location.node.asList().addAt(value, index);

        } catch (NumberFormatException e) {
          throw new IllegalStateException("Not a valid list index");
        }

      }

    } catch (SException e) {
      throw new IllegalStateException(e);
    }


  }

  /**
   * Build a view of the SNode tree in runtime's native data format. This method
   * is meant to generate a Javascript view of the SNode tree for Javascript
   * runtime.
   * <p>
   * <br>
   * For non Javascript runtime we discourage to implement this method and to
   * use SNode types instead.
   *
   * @param root
   * @param path
   * @return a representation of the SNode tree in native data format (e.g.
   *         Javascript)
   */
  public static Object get(SNode root, String path) {

    Preconditions.checkArgument(root != null, "SNode argument is null");

    SNode node;
    Object result = null;
    try {
      node = node(root, path);
      result = ModelFactory.instance.getJsonBuilder(node).build();
    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    return result;

  }

  /**
   * Add a property a list.
   *
   * @param root
   * @param path
   * @param value
   */
  public static void push(SNode root, String path, Object value) {

    Preconditions.checkArgument(path != null && !path.isEmpty(), "Path is empty or null");
    Preconditions.checkArgument(value != null, "Can't set a null value");

    try {
      SNodeLocator.Location location = SNodeLocator.locate(root, new PathNavigator(path));

      if (location.isJsonObject() || location.isJsonProperty()) {
        throw new IllegalStateException("Node can't be mutated");
      }

      if (location.node instanceof SList) {
        location.node.asList().add(value);

      } else {
        throw new IllegalStateException("Node is not a list");
      }

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  /** Returns and delete last element of a list */
  public static Object pop(SNode root, String path) {

    Preconditions.checkArgument(path != null && !path.isEmpty(), "Path is empty or null");

    try {
      SNodeLocator.Location location = SNodeLocator.locate(root, new PathNavigator(path));

      if (location.isJsonObject() || location.isJsonProperty()) {
        throw new IllegalStateException("Node can't be mutated");
      }

      if (location.node instanceof SList) {
        int lastIndex = location.node.asList().size() - 1;
        Object result = location.node.asList().pick(lastIndex);
        location.node.asList().remove(lastIndex);
        return result;
      }

      throw new IllegalStateException("Node is not a list");

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  public static void delete(SNode root, String path) {

    Preconditions.checkArgument(path != null && !path.isEmpty(), "Path is empty or null");

    String property = null;
    int separatorPos = path.lastIndexOf(".");
    if (separatorPos != -1) {
      property = path.substring(separatorPos + 1);
      path = path.substring(0, separatorPos);
    } else {
      property = path;
      path = "";
    }

    try {
      SNodeLocator.Location location = SNodeLocator.locate(root, new PathNavigator(path));

      if (location.isJsonObject() || location.isJsonProperty()) {
        throw new IllegalStateException("Node can't be mutated");
      }

      if (location.node instanceof SMap) {

        location.node.asMap().remove(property);

      } else if (location.node instanceof SList) {

        try {

          int index = Integer.valueOf(property);
          location.node.asList().remove(index);

        } catch (NumberFormatException e) {
          throw new IllegalStateException("Not a valid list index");
        }

      }

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  public static int length(SNode root, String path) {

    try {
      SNode node = node(root, path);

      if (node instanceof SMap)
        return node.asMap().size();

      if (node instanceof SList)
        return node.asList().size();

      throw new IllegalStateException("Node is not a container");

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  public static boolean contains(SNode root, String path, String property) {


    Preconditions.checkArgument(path != null && !path.isEmpty(), "Path is empty or null");
    Preconditions.checkArgument(property != null && !property.isEmpty(),
        "Property is empty or null");

    try {
      SNodeLocator.Location location = SNodeLocator.locate(root, new PathNavigator(path));

      if (location.node instanceof SMap) {

        return location.node.asMap().has(property);

      } else if (location.node instanceof SList) {

        try {

          int index = Integer.valueOf(property);
          return index >= 0 && index < location.node.asList().size();

        } catch (NumberFormatException e) {
          throw new IllegalStateException("Not a valid list index");
        }

      }

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    throw new IllegalStateException("Node is not a container");
  }


  /**
   * Look up a node referenced by a path starting in a root node.
   *
   * @param root
   * @param path
   * @return
   * @throws SException
   */
  public static SNode node(SNode root, String path) throws SException {

    SNodeLocator.Location location = SNodeLocator.locate(root, new PathNavigator(path));

    if (location.isJsonObject()) {

      // Transform Json Object in a tree of local nodes
      return ModelFactory.instance.getSNodeBuilder().build(((SPrimitive) location.node).getValue());

    } else if (location.isJsonProperty()) {

      // Dive into Json node as SNode tree
      SNode jsonRoot = ModelFactory.instance.getSNodeBuilder()
          .build(((SPrimitive) location.node).getValue());
      return node(jsonRoot, location.subPath);

    } else {

      return location.node;

    }

  }


  //
  // --------------------------------------------------------------
  //

  /**
   * Return a property in the object as node type
   *
   * @throws SException
   */
  SNode node(String path) throws SException;

  /**
   * Set or update a property.
   *
   * @param path
   * @param value
   */
  void set(String path, Object value);

  /**
   * Get a property or an array element if index is provided as last part of the
   * path.
   *
   * @param path
   * @return
   */
  Object get(String path);

  /**
   * Add a new value to an array.
   *
   * @param path
   * @param value
   */
  void push(String path, Object value);

  /**
   * Returns and delete the last value of an array;
   *
   * @param path
   * @return
   */
  Object pop(String path);

  /**
   * Delete a property or element in an array.
   *
   * @param path
   */
  void delete(String path);

  /**
   * Returns the number of properties of an object or number of elements of an
   * array.
   *
   * @param path
   * @return
   */
  int length(String path);

  /**
   * Check whether a property exists.
   *
   * @param path
   * @param property
   * @return
   */
  boolean contains(String path, String property);

  /**
   * Accepts a visitor.
   *
   * @param visitor
   */
  @SuppressWarnings("rawtypes")
  @JsIgnore
  void accept(SVisitor visitor);

  /**
   * @return this node as a Map. Throws exception otherwise.
   */
  SMap asMap();

  /**
   * @return this node as a Map. Throws exception otherwise.
   */
  SList<? extends SNode> asList();

  /**
   * @return this node as a Map. Throws exception otherwise.
   */
  String asString();

  /**
   * @return this node as a Map. Throws exception otherwise.
   */
  double asDouble();

  /**
   * @return this node as a Map. Throws exception otherwise.
   */
  int asInt();

  /**
   * @return this node as a Map. Throws exception otherwise.
   */
  boolean asBoolean();

  /**
   * @return this node as Text. Throws exception otherwise.
   */
  SText asText();
}
