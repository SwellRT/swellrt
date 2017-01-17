package org.swellrt.beta.client.wave.atmosphere;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.swellrt.beta.client.wave.WaveSocket;
import org.swellrt.beta.client.wave.WaveSocket.WaveSocketCallback;

/**
 * A WaveSocketCallback implementations adding specific Atmosphere WebSocket
 * subprotocol features to the original Wave WebSocket subprotocol.
 *
 *
 * This implementation expects following atmosphere features enabled:
 * <ul>
 * <li>Heart beat messages</li>
 * <li>Track message size + Base64 message encoding</li>
 * </ul>
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveSocketAtmosphereCallback implements WaveSocket.WaveSocketCallback {

  private static final Logger Log = Logger.getLogger(WaveSocketAtmosphereCallback.class
      .getSimpleName());


  /** Header name for extensions of the wave socket protocol */
  private static final String EXT_RESPONSE_HEADER = "X-RESPONSE:";

  private static final String EXT_RESPONSE_CLIENT_NOT_SUPPORTED = "CLIENT_NOT_SUPPORTED";
  private static final String EXT_RESPONSE_INVALID_SESSION = "INVALID_SESSION";


  final static int WAVE_MESSAGE_SEPARATOR = '|';
  final static String WAVE_MESSAGE_END_MARKER = "}|";

  private final WaveSocketCallback delegate;

  public WaveSocketAtmosphereCallback(WaveSocketCallback delegate) {
    this.delegate = delegate;
  }


  private boolean isPackedWaveMessage(String message) {
    return message.indexOf(WAVE_MESSAGE_SEPARATOR) == 0;
  }


  private List<String> unpackWaveMessages(String packedMessage) {

    List<String> messages = new ArrayList<String>();

    if (isPackedWaveMessage(packedMessage)) {

      while (packedMessage.indexOf(WAVE_MESSAGE_SEPARATOR) == 0 && packedMessage.length() > 1) {
        packedMessage = packedMessage.substring(1);
        int marker = packedMessage.indexOf(WAVE_MESSAGE_END_MARKER);
        String splitMessage = packedMessage.substring(0, marker + 1);
        messages.add(splitMessage);
        packedMessage = packedMessage.substring(marker + 1);
      }
    }

    return messages;
  }


  @Override
  public void onConnect() {
    delegate.onConnect();
  }

  @Override
  public void onDisconnect() {
    delegate.onDisconnect();
  }

  /**
   * Decode Base64 string using browser native functions to avoid issues on
   * large encoded strings.
   *
   * It expects an UTF-8 string.
   *
   * @param m
   * @return
   */
  protected native String decodeBase64(String m) /*-{
    var decoded = $wnd.atob( m );
    return decodeURIComponent( escape( decoded ) );
  }-*/;


  @Override
  public void onMessage(String message) {

    try {

      // Decode from Base64 because of Atmosphere Track Message Lenght server
      // feauture
      // NOTE: no Charset is specified, so this relies on UTF-8 as default
      // charset
      String decoded = decodeBase64(message);


      // Ignore heart-beat messages
      // NOTE: is heart beat string always " "?
      if (decoded == null || decoded.isEmpty() || decoded.startsWith(" ")
          || decoded.startsWith("  ")) return;


      if (isPackedWaveMessage(decoded)) {
        List<String> unpacked = unpackWaveMessages(decoded);
        for (String s : unpacked) {
          dispatch(s);
        }

      } else {
        dispatch(decoded);
      }

    } catch (Exception e) {
      delegate.onError(e);
    }

  }

  /**
   * Check a message for extension headers and dispatch it to the
   * {@link WaveSocket.WaveSocketCallback}
   *
   * @param message a Wave RPC protocol's message maybe including extensions
   */
  private void dispatch(String message) {

    if (message.startsWith(EXT_RESPONSE_HEADER)) {
      String response = message.substring(EXT_RESPONSE_HEADER.length());
      delegate.onError(new Exception(response));
    } else {
      delegate.onMessage(message);
    }

  }


  @Override
  public void onError(Throwable t) {
    delegate.onError(t);
  }

}
