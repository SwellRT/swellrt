package org.swellrt.beta.model.wave.unmutable;

import java.util.Map;

public class UnmutableMap implements UnmutableNode {

  protected final Map<String, UnmutableNode> map;

  protected UnmutableMap(Map<String, UnmutableNode> map) {
    this.map = map;
  }

}
