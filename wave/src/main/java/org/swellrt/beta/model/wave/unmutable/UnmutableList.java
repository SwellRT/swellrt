package org.swellrt.beta.model.wave.unmutable;

import java.util.List;

public class UnmutableList implements UnmutableNode {

  private final List<UnmutableNode> list;

  public UnmutableList(List<UnmutableNode> list) {
    super();
    this.list = list;
  }

}
