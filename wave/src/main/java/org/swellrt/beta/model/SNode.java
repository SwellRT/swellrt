package org.swellrt.beta.model;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Node")
public interface SNode {

  static String[] splitPath(String path) {
    int pathSepPos = path.lastIndexOf('.');
    String key = path.substring(pathSepPos + 1, path.length());
    if (pathSepPos >= 0)
      path = path.substring(0, path.lastIndexOf('.'));
    else
      path = "";

    return new String[] { path, key };
  }

  public static void set(SNode root, String path, Object value) {

    SNode node;

    String[] pathParts = null;
    String key = null;

    if (path != null) {
      pathParts = splitPath(path);
      path = pathParts[0];
      key = pathParts[1];
    }

    try {
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, root);

      if (node == null)
        throw new RuntimeException("Node not found at " + path);

      if (node instanceof SMap) {
        SMap map = (SMap) node;
        map.put(key, value);

      } else if (node instanceof SPrimitive) {

        // Move this logic to a platform dependent class

        SPrimitive primitive = (SPrimitive) node;

        // We only can put objects inside JSO primitives with container.
        SPrimitive container = primitive.getContainer();
        if (container == null)
          throw new RuntimeException("Property " + key + " can't be set in " + path);

        Object actualValue = null;
        if (value instanceof SNode) {
          actualValue = PlatformBasedFactory.getViewBuilderForNode().getView((SNode) value);
        } else if (value instanceof JavaScriptObject) {
          actualValue = value;
        } else {
          actualValue = value;
        }

        // Update or set value inside JSO
        JavaScriptObject jso = (JavaScriptObject) primitive.value();
        JsoView jsv = JsoView.as(jso);
        jsv.setObject(key, actualValue);

        // Get the parent node of the JSO node and update
        String p = primitive.getContainerPath();
        String containerParentKey = p.lastIndexOf(".") >= 0
            ? p.substring(p.lastIndexOf(".") + 1, p.length()) : p;
        if (containerParentKey == null || containerParentKey.isEmpty())
          throw new RuntimeException("Value can't be set in " + path + "." + key);

        SNode containerParent = container.getParent();
        if (containerParent instanceof SMap) {
          SMap m = (SMap) containerParent;
          m.put(containerParentKey, container);
        } else if (containerParent instanceof SList) {
          SList l = (SList) containerParent;
          l.add(container, Integer.valueOf(containerParentKey));
        } else {
          // this shouldn't happen
          throw new RuntimeException("Value can't be set in " + path + "." + key);
        }

      } else {
        throw new RuntimeException("Property " + key + " can't be set in " + path);
      }

    } catch (SException e1) {
      throw new RuntimeException(e1);
    }

  }

  public static Object get(SNode root, String path) {

    SNode node;

    String[] pathParts = null;
    String key = null;

    try {

      if (path != null) {
        pathParts = splitPath(path);
        path = pathParts[0];
        key = pathParts[1];
        node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, root);
      } else {
        return PlatformBasedFactory.getViewBuilderForNode().getView(root);
      }


      if (node == null)
        throw new RuntimeException("Node not found at " + path);

      if (node instanceof SMap) {
        SMap map = (SMap) node;
        return PlatformBasedFactory.getViewBuilderForNode().getView(map.node(key));

      } else if (node instanceof SList) {
        @SuppressWarnings("rawtypes")
        SList list = (SList) node;
        int index = Integer.valueOf(key);
        return PlatformBasedFactory.getViewBuilderForNode().getView(list.node(index));

      } else if (node instanceof SPrimitive) {

        SPrimitive primitive = (SPrimitive) node;
        if (primitive.getType() == SPrimitive.TYPE_JSO) {
          return PlatformBasedFactory.extractNode((JavaScriptObject) primitive.value(), key);
        } else {
          return primitive.value();
        }

      } else {
        throw new RuntimeException("Property " + key + " can't be retrieved");
      }

    } catch (SException e1) {
      throw new RuntimeException(e1);
    } catch (NumberFormatException e2) {
      throw new RuntimeException(key + " is not a valid array index");
    }
  }

  public static void push(SNode root, String path, Object value, Object index) {

    SNode node;
    try {
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, root);

      if (node == null)
        throw new RuntimeException("Node not found at " + path);

      if (node instanceof SList) {
        @SuppressWarnings("rawtypes")
        SList list = (SList) node;
        list.add(value, index);

      } else {
        throw new RuntimeException("Value can't be pushed in " + path);
      }

    } catch (SException e1) {
      throw new RuntimeException(e1);
    }

  }

  public static Object pop(SNode root, String path) {

    SNode node;
    try {
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, root);

      if (node == null)
        throw new RuntimeException("Node not found at " + path);

      if (node instanceof SList) {
        @SuppressWarnings("rawtypes")
        SList list = (SList) node;
        Object object = PlatformBasedFactory.getViewBuilderForNode()
            .getView(list.node(list.size() - 1));
        list.remove(list.size() - 1);
        return object;
      } else {
        throw new RuntimeException("Value can't be pop from " + path);
      }

    } catch (SException e1) {
      throw new RuntimeException(e1);
    }
  }

  public static void delete(SNode root, String path) {

    SNode node;
    String[] pathParts = splitPath(path);
    path = pathParts[0];
    String key = pathParts[1];

    try {
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, root);

      if (node == null)
        throw new RuntimeException("Node not found at " + path);

      if (node instanceof SMap) {
        SMap map = (SMap) node;
        map.remove(key);

      } else if (node instanceof SList) {

        @SuppressWarnings("rawtypes")
        SList list = (SList) node;
        int index = Integer.valueOf(key);
        list.remove(index);

      } else {
        throw new RuntimeException("Property " + key + " can't be delete");
      }

    } catch (SException e1) {
      throw new RuntimeException(e1);
    } catch (NumberFormatException e2) {
      throw new RuntimeException(key + " is not a valid array index");
    }
  }

  public static int length(SNode root, String path) {

    SNode node;

    try {
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, root);

      if (node == null)
        return 0;

      if (node instanceof SMap) {
        SMap map = (SMap) node;
        return map.size();

      }

      if (node instanceof SList) {
        @SuppressWarnings("rawtypes")
        SList list = (SList) node;
        return list.size();
      }

    } catch (SException e1) {
      throw new RuntimeException(e1);
    }

    return 0;

  }

  public static boolean contains(SNode root, String path, String property) {

    SNode node;

    try {
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, root);

      if (node == null)
        return false;

      if (node instanceof SMap) {
        SMap map = (SMap) node;
        return map.has(property);

      }

      if (node instanceof SList) {
        @SuppressWarnings("rawtypes")
        SList list = (SList) node;
        int index = Integer.valueOf(property);
        return 0 <= index && index < list.size();
      }

    } catch (SException e1) {
      throw new RuntimeException(e1);
    } catch (NumberFormatException e2) {
      throw new RuntimeException(property + " is not a valid array index");
    }

    return false;
  }



  //
  // --------------------------------------------------------------
  //

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
   * @param index
   */
  void push(String path, Object value, @JsOptional Object index);

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


}
