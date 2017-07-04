package org.swellrt.beta.common;

public class PathWalker {

  protected final String originalPath;
  protected String path;
  protected String consumedPath;

  public PathWalker(String path) {
    this.originalPath = path;
    this.path = path;
    this.consumedPath = "";
  }

  /** @return next path element in the left */
  public String nextPathElement() {

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

  /** @return rest path to be processed */
  public String get() {
    return path;
  }

  /** @return part of the path already walked */
  public String getConsumed() {
    return consumedPath;
  }
}
