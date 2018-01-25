package org.swellrt.beta.client.wave;

import org.swellrt.beta.client.rest.JsonParser;
import org.swellrt.beta.client.wave.ws.WebSocket;
import org.swellrt.beta.model.json.SJsonObject;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.scheduler.TimerService;

/**
 * Platform-dependent global dependencies regarding Wave stuff.
 */
public class WaveDeps {

  public static interface IntRandomGenerator {
    int nextInt();
  }

  public static interface ColorGenerator {
    RgbColor getColor(String id);
  }

  public static WaveLoader.Factory loaderFactory = null;

  public static IntRandomGenerator intRandomGeneratorInstance = null;

  public static Log.Factory logFactory = null;

  public static ProtocolMessageUtils protocolMessageUtils = null;

  public static VersionSignatureManager versionSignatureManager = new VersionSignatureManager();

  public static WebSocket.Factory websocketFactory = null;

  public static TimerService lowPriorityTimer = null;

  public static TimerService mediumPriorityTimer = null;

  public static TimerService highPriorityTimer = null;

  public static JsonParser json = null;

  public static SJsonObject.Factory sJsonFactory = null;

  public static ColorGenerator colorGeneratorInstance = null;


  static final char[] WEB64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();

  public static String getRandomBase64(int length) {
    StringBuilder result = new StringBuilder(length);
    int bits = 0;
    int bitCount = 0;
    while (result.length() < length) {
      if (bitCount < 6) {
        bits = WaveDeps.intRandomGeneratorInstance.nextInt();
        bitCount = 32;
      }
      result.append(WEB64_ALPHABET[bits & 0x3F]);
      bits >>= 6;
      bitCount -= 6;
    }
    return result.toString();
  }
}
