package org.swellrt.beta.client;

import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ProfileManager;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "ServiceConnection")
public interface ServiceConnection {

  @JsFunction
  public interface ConnectionHandler {
    void exec(String state, SException e);
  }

  @JsProperty
  ProfileManager getProfilesManager();

  void addConnectionHandler(ConnectionHandler h);

  void removeConnectionHandler(ConnectionHandler h);

}
