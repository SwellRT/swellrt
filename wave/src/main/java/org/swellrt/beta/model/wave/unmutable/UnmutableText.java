package org.swellrt.beta.model.wave.unmutable;

import org.waveprotocol.wave.model.document.Document;

public class UnmutableText implements UnmutableNode {

  private final Document document;

  public UnmutableText(Document document) {
    super();
    this.document = document;
  }

}
