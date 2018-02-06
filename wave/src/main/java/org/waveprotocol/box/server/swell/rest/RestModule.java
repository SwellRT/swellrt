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

  static final String WAVE_ID = "waveid";
  static final String WAVELET_ID = "waveletid";
  static final String DOC_ID = "docid";

  static final String VERSION = "v";
  static final String VERSION_START = "vs";
  static final String VERSION_END = "ve";
  static final String NUM_OF_RESULTS = "l";
  static final String RETURN_OPS = "ops";

  static final String VERSION_PATH_SEGMENT = "{version}";
  static final String WAVE_PATH_SEGMENT = "wave/{waveid:.*/.*}";
  static final String WAVELET_PATH_SEGMENT = "wavelet/{waveletid:.*/.*}";
  static final String DOC_PATH_SEGMENT = "doc/{docid}";

  static final String DOC_PATH = "/" + WAVE_PATH_SEGMENT + "/" + WAVELET_PATH_SEGMENT + "/"
      + DOC_PATH_SEGMENT;
  static final String WAVELET_PATH = "/" + WAVE_PATH_SEGMENT + "/" + WAVELET_PATH_SEGMENT;
  static final String WAVE_PATH = "/" + WAVE_PATH_SEGMENT;

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
