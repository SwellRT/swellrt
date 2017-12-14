package org.swellrt.beta.model.java;

import org.swellrt.beta.model.JsonToSNode;
import org.swellrt.beta.model.SNode;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gson.JsonElement;

public class JsonToSNodeJava implements JsonToSNode {

  @Override
  public SNode build(Object json) {
    Preconditions.checkArgument(json != null, "Null JSON object");
    Preconditions.checkArgument(json instanceof JsonElement, "Expected a Gson's JsonElement");
    return SNodeGson.castToSNode((JsonElement) json);
  }

}
