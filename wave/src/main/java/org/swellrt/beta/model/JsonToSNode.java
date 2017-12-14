package org.swellrt.beta.model;

public interface JsonToSNode {

  /**
   * Translates a JSON tree in a SNode tree. JSON object's type is platform
   * dependent.
   * <li>For Javascript runtime is {@code JavaScriptObject}.</li>
   * <li>For Java runtime is {@code Gson} {@code JsonElement}</li>
   *
   * @param json
   *          a JSON node.
   * @return a {@link SNode} tree
   */
  SNode build(Object json);

}
