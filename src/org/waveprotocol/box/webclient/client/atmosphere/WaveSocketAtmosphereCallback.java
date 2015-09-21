package org.waveprotocol.box.webclient.client.atmosphere;

import org.waveprotocol.box.webclient.client.WaveSocket;
import org.waveprotocol.box.webclient.client.WaveSocket.WaveSocketCallback;
import org.waveprotocol.wave.client.events.ClientEvents;
import org.waveprotocol.wave.client.events.NetworkStatusEvent;
import org.waveprotocol.wave.client.events.NetworkStatusEvent.ConnectionStatus;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.util.CharBase64;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public class WaveSocketAtmosphereCallback implements WaveSocket.WaveSocketCallback {

  private static final Logger Log = Logger.getLogger(WaveSocketAtmosphereCallback.class
      .getSimpleName());


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


  @Override
  public void onMessage(String message) {

    try {

      // Decode from Base64 because of Atmosphere Track Message Lenght server
      // feauture
      // NOTE: no Charset is specified, so this relies on UTF-8 as default
      // charset
      String decoded = new String(CharBase64.decode(message));


      // Ignore heart-beat messages
      // NOTE: is heart beat string always " "?
      if (decoded == null || decoded.isEmpty() || decoded.startsWith(" ")
          || decoded.startsWith("  ")) return;


      if (isPackedWaveMessage(decoded)) {
        List<String> unpacked = unpackWaveMessages(decoded);
        for (String s : unpacked) {
          delegate.onMessage(s);
        }

      } else {
        delegate.onMessage(decoded);
      }

    } catch (Base64DecoderException e) {
      Log.severe(e.getMessage());
      // Errors here should passed to WaveWebSocket, instead of relaying on
      // client events.
      ClientEvents.get().fireEvent(new NetworkStatusEvent(ConnectionStatus.PROTOCOL_ERROR));
    }

  }


}
