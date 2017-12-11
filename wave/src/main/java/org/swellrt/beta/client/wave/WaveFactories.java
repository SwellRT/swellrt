package org.swellrt.beta.client.wave;

import org.swellrt.beta.client.platform.web.browser.JSON;
import org.swellrt.beta.client.rest.JsonParser;
import org.swellrt.beta.client.wave.ws.WebSocket;
import org.waveprotocol.wave.client.scheduler.TimerService;

public class WaveFactories {

  public static interface Random {
    int nextInt();
  }

  public static WaveLoader.Factory loaderFactory = null;

  public static Random randomGenerator = null;

  public static Log.Factory logFactory = null;

  public static ProtocolMessageUtils protocolMessageUtils = null;

  public static VersionSignatureManager versionSignatureManager = new VersionSignatureManager();

  public static WebSocket.Factory websocketFactory = null;

  public static TimerService lowPriorityTimer = null;

  public static TimerService mediumPriorityTimer = null;

  public static TimerService highPriorityTimer = null;

  public static JsonParser json = new JsonParser() {

    @Override
    public <T, R extends T> T parse(String json, Class<R> dataType) {
      return JSON.<T> parse(json);
    }

  };
}
