package org.swellrt.beta.model;

import org.swellrt.beta.common.PathNavigator;
import org.swellrt.beta.common.SException;

public class SNodeLocator {

  public static class Location {

    /**
     * The node pointed by the source path or a SPrimitive-JSON node containing
     * the actual node
     */
    public SNode node;
    /**
     * A sub path inside a SPrimitive-JSON container node.
     */
    public String subPath;

    public boolean isJsonProperty() {
      return (node instanceof SPrimitive) && (((SPrimitive) node).isJso())
          && (subPath != null);
    }

    public boolean isJsonObject() {
      return (node instanceof SPrimitive) && (((SPrimitive) node).isJso())
          && (subPath == null);
    }

    public Location(SNode node, String subPath) {
      super();
      this.node = node;
      this.subPath = subPath;
    }

  }

  public static Location locate(SNode node, PathNavigator path) throws SException {

    if (path.currentPath().isEmpty()) {
      return new Location(node, null);
    }

    if (node instanceof SPrimitive) {

      SPrimitive pnode = (SPrimitive) node;

      if (pnode.isJso()) {
        return new Location(node, path.currentPath());
      } else {
        throw new SException(SException.PATH_NOT_FOUND);
      }

    } else {

      SNode nextNode = null;

      if (node instanceof SMap) {

        String property = path.next();
        SMap map = (SMap) node;
        nextNode = map.pick(property);

      } else if (node instanceof SList) {

        if (!path.nextIsInt()) {
          throw new SException(SException.PATH_NOT_FOUND);
        }

        @SuppressWarnings("unchecked")
        SList<? extends SNode> list = (SList<? extends SNode>) node;
        int index = path.nextInt();
        nextNode = list.pick(index);

      } else {

        throw new SException(SException.PATH_NOT_FOUND);
      }

      return locate(nextNode, path);
    }

  }

}
