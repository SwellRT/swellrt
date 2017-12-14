package org.swellrt.beta.common;

/**
 * Utility class to go through a path expression.
 *
 * @author pablojan@gmail.com
 *
 */
public class PathNavigator {

  protected final String originalPath;
  protected String path;
  protected String consumedPath;

  public PathNavigator(String path) {
    this.originalPath = path;
    this.path = path;
    this.consumedPath = "";
  }

  /** @return next element in the path */
  public String next() {

    if (path.isEmpty())
      return null;

    String element = null;

    int separatorPos = path.indexOf(".");
    if (separatorPos == -1) {
      separatorPos = path.length();
      element = path.substring(0, separatorPos);
      path = "";
    } else {
      element = path.substring(0, separatorPos);
      path = path.substring(separatorPos + 1);
    }

    if (!consumedPath.isEmpty())
      consumedPath += ".";
    consumedPath += element;
    return element;
  }

  public boolean nextIsInt() {

    if (path.isEmpty())
      return false;

    String element = null;

    int separatorPos = path.indexOf(".");
    if (separatorPos == -1) {
      separatorPos = path.length();
      element = path.substring(0, separatorPos);
    } else {
      element = path.substring(0, separatorPos);
    }

    try {
      @SuppressWarnings("unused")
      int asInt = Integer.valueOf(element);
      return true;
    } catch (Exception e) {
      return false;
    }

  }

  /** @return rest path to be processed */
  public String path() {
    return path;
  }

  /** @return part of the path already walked */
  public String consumedPath() {
    return consumedPath;
  }
}
