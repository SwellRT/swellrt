package org.waveprotocol.box.server.swell.rest;

import javax.ws.rs.core.CacheControl;

import org.waveprotocol.box.server.rpc.ServerRpcProvider;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Guice Module with all REST services provided by Swell. Check out
 * {@link ServerRpcProvider#startWebSocketServer()} for RestEasy services
 * startup.
 *
 */
public class RestModule implements Module {

  static final CacheControl CACHE_24H = new CacheControl();
  static final CacheControl CACHE_NO_STORE = new CacheControl();

  static {
    CACHE_24H.setMaxAge(86400);
    CACHE_NO_STORE.setNoStore(true);
  }

  @Override
  public void configure(Binder binder) {

    binder.bind(DataRestResources.class);
    binder.bind(NamingRestResources.class);

  }

}
