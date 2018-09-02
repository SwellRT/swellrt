package org.swellrt.beta.client.platform.java;

import org.swellrt.beta.client.ServiceConfigProvider;

public class JavaServiceConfig implements ServiceConfigProvider {

  @Override
  public boolean getCaptureExceptions() {
    throw new IllegalStateException("not available");

  }

  @Override
  public int getWebsocketHeartbeatInterval() {
    throw new IllegalStateException("not available");
  }

  @Override
  public int getWebsocketHeartbeatTimeout() {
    throw new IllegalStateException("not available");

  }

  @Override
  public boolean getWebsocketDebugLog() {
    throw new IllegalStateException("not available");

  }

  @Override
  public int getTrackPresencePingRateMs() {
    throw new IllegalStateException("not available");
  }

}
