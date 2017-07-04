package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;

public interface PathNodeExtractor {

  public SNode getNode(String path, SNode root) throws SException;

}
