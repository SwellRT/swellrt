package org.waveprotocol.box.server.swell.rest;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Guice Module with all REST services provided by Swell.
 *
 */
public class RestModule implements Module {

  @Override
  public void configure(Binder binder) {

    binder.bind(RawResource.class);

  }

}
