package org.swellrt.beta.model.js;

import org.swellrt.beta.model.SNodeBuilder;
import org.swellrt.beta.model.SNode;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;

public class SNodeBuilderJs implements SNodeBuilder {

  @Override
  public SNode build(Object json) {
    Preconditions.checkArgument(json != null, "Json object can't be null");
    Preconditions.checkArgument(json instanceof JavaScriptObject, "Not a JavaScriptObject");
    return SNodeJs.castToSNode((JavaScriptObject) json);
  }

}
