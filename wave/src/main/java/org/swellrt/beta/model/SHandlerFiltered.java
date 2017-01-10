package org.swellrt.beta.model;

import jsinterop.annotations.JsIgnore;

public final class SHandlerFiltered implements SHandler {
  
  private final SHandler delegate;
  private final String keyFilter;
  
  @JsIgnore
  public SHandlerFiltered(SHandler delegate, String keyFilter) {
    this.delegate = delegate;
    this.keyFilter = keyFilter;
  }
  
  @Override
  public boolean exec(SEvent e) {

    if (e.getKey().equals(keyFilter)) {
      return delegate.exec(e);
    }
    
    return true;
  }

}
