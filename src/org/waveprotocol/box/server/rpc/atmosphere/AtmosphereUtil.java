package org.waveprotocol.box.server.rpc.atmosphere;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.util.IOUtils;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

/**
 * Util methods to work with Atmosphere resources
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AtmosphereUtil {

  private static final Log LOG = Log.get(AtmosphereUtil.class);

  private static final String CHARSET = "UTF-8";
  private static final String SEPARATOR = "|";

  /**
   * Flush an {@link AtmosphereResource} properly according to the underlying
   * transport protocol used.
   *
   * @param resource
   */
  public static void flush(AtmosphereResource resource) {
    try {

      resource.getResponse().flushBuffer();

      switch (resource.transport()) {
        case JSONP:
        case LONG_POLLING:
        case POLLING:
          resource.resume();
          break;
        case WEBSOCKET:
        case STREAMING:
        case SSE:
          resource.getResponse().getOutputStream().flush();
          break;
        default:
          LOG.info("Unknown transport");
          break;
      }
    } catch (IOException e) {
      LOG.warning("Error resuming resource response", e);
    }
  }

  /**
   * Packing messages ensures that a Wave message order is preserved when server
   * is delivering messages through atmosphere long-polling. So far I wasn't
   * able to avoid this.
   *
   * @param messages
   * @return
   * @throws UnsupportedEncodingException
   */
  public static byte[] packMessages(List<Object> messages) throws UnsupportedEncodingException {

    StringBuilder sb = new StringBuilder();
    sb.append(SEPARATOR);
    for (Object obj : messages)
      sb.append((String) obj).append(SEPARATOR);


    return sb.toString().getBytes(CHARSET);
  }

  public static void writeMessage(AtmosphereResource resource, Object message) throws IOException {

    AtmosphereResponse response = resource.getResponse();

    // Set content type before do response.getWriter()
    // http://docs.oracle.com/javaee/5/api/javax/servlet/ServletResponse.html#setContentType(java.lang.String)
    response.setContentType("text/plain; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    if (message.getClass().isArray()) {

      List<Object> list = Arrays.asList(message);
      response.write(packMessages(list));

    } else if (message instanceof List) {

      @SuppressWarnings("unchecked")
      List<Object> list = List.class.cast(message);
      response.write(packMessages(list));

    } else if (message instanceof String) {

      String str = (String) message;
      response.write(str.getBytes(CHARSET));
    }


    flush(resource);
  }

  public static String readMessage(AtmosphereResource resource) {
    return IOUtils.readEntirely(resource).toString();
  }

  public static boolean isWebsocketProtocol(AtmosphereResource resource) {
    return resource.transport().equals(TRANSPORT.WEBSOCKET.name());
  }

  public static boolean isLongPollingProtocol(AtmosphereResource resource) {
    return resource.transport().equals(TRANSPORT.LONG_POLLING)
        || resource.transport().equals(TRANSPORT.POLLING);
  }

}
