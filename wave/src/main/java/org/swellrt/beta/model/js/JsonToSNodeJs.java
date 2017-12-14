package org.swellrt.beta.model.js;

import org.swellrt.beta.model.JsonToSNode;
import org.swellrt.beta.model.SNode;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;

public class JsonToSNodeJs implements JsonToSNode {

  @Override
  public SNode build(Object json) {
    Preconditions.checkArgument(json != null, "Json object can't be null");
    Preconditions.checkArgument(json instanceof JavaScriptObject, "Not a JavaScriptObject");
    return SNodeJs.castToSNode((JavaScriptObject) json);
  }

}
