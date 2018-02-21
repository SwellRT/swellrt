package org.swellrt.beta.client.platform.java;

import org.swellrt.beta.client.wave.ProtocolMessageUtils;
import org.waveprotocol.box.common.comms.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.ProtocolAuthenticationResult;
import org.waveprotocol.box.common.comms.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitResponse;
import org.waveprotocol.box.common.comms.ProtocolWaveletUpdate;
import org.waveprotocol.box.common.comms.gson.ProtocolAuthenticateGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolOpenRequestGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolSubmitRequestGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolSubmitResponseGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolWaveletUpdateGsonImpl;
import org.waveprotocol.box.webclient.common.WaveletOperationSerializer;
import org.waveprotocol.wave.communication.gson.GsonException;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.federation.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.gson.ProtocolDocumentOperationGsonImpl;
import org.waveprotocol.wave.federation.gson.ProtocolWaveletDeltaGsonImpl;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class JavaProtocolMessageUtils extends ProtocolMessageUtils {

  private static Gson gson = new Gson();
  private static JsonParser parser = new JsonParser();

  public static final class JsonMessageWrapper implements MessageWrapper {

    private final JsonObject jso;

    private JsonMessageWrapper(JsonObject jso) {
      this.jso = jso;
    }

    private JsonMessageWrapper(int seqno, String type, JsonObject message) {
      this.jso = new JsonObject();
      jso.add("sequenceNumber", new JsonPrimitive(seqno));
      jso.add("messageType", new JsonPrimitive(type));
      jso.add("message", message);

    }

    public String toJson() {
      return gson.toJson(jso);
    }

    @Override
    public int getSequenceNumber() {
      return jso.getAsJsonPrimitive("sequenceNumber").getAsInt();
    }

    @Override
    public String getMessageType() {
      return jso.getAsJsonPrimitive("messageType").getAsString();
    }

    @Override
    public JsonObject getMessage() {
      return jso.getAsJsonObject("message");
    }

    @Override
    public boolean isProtocolWaveletUpdate() {
      return MSG_WAVELET_UPDATE.equals(getMessageType());
    }

    @Override
    public boolean isProtocolSubmitResponse() {
      return MSG_SUBMIT_RESPONSE.equals(getMessageType());
    }

    @Override
    public boolean isProtocolAuthenticationResult() {
      return MSG_AUTH_RESULT.equals(getMessageType());
    }

    @Override
    public boolean isRpcFinished() {
      return MSG_RPC_FINISHED.equals(getMessageType());
    }

  }

  public static final class JsonRpcFinished implements RpcFinished {

    private final JsonObject jso;

    protected JsonRpcFinished(JsonObject jso) {
      this.jso = jso;
    }

    public boolean hasFailed() {
      return jso.get("1").getAsBoolean();
    }

    public boolean hasErrorText() {
      return jso.has("2");
    }

    public String getErrorText() {
      return jso.get("2").getAsString();
    }

    public ChannelException getChannelException() {
      ChannelException e = null;
      if (hasErrorText()) {
        e = ChannelException.deserialize(getErrorText());
      }
      return e;
    }

  }

  @Override
  public MessageWrapper parseWrapper(String json) throws ParseException {
    JsonElement messageWrapper = parser.parse(json);
    return new JsonMessageWrapper(messageWrapper.getAsJsonObject());
  }

  @Override
  public ProtocolWaveletUpdate unwrapWaveletUpdate(MessageWrapper message) {
    ProtocolWaveletUpdateGsonImpl o = new ProtocolWaveletUpdateGsonImpl();
    try {
      o.fromGson(((JsonMessageWrapper) message).getMessage(), gson, null);
    } catch (GsonException e) {
      throw new IllegalStateException(e);
    }
    return o;
  }

  @Override
  public ProtocolSubmitResponse unwrapSubmitResponse(MessageWrapper message) {
    ProtocolSubmitResponseGsonImpl o = new ProtocolSubmitResponseGsonImpl();
    try {
      o.fromGson(((JsonMessageWrapper) message).getMessage(), gson, null);
    } catch (GsonException e) {
      throw new IllegalStateException(e);
    }
    return o;
  }

  @Override
  public ProtocolAuthenticationResult unwrapAuthResult(MessageWrapper message) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public ProtocolMessageUtils.RpcFinished unwrapRpcFinished(MessageWrapper message) {
    return new JsonRpcFinished(((JsonMessageWrapper) message).getMessage());
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolSubmitRequest message) {
    ProtocolSubmitRequestGsonImpl o = new ProtocolSubmitRequestGsonImpl(message);
    return new JsonMessageWrapper(seqNum, MSG_SUBMIT_REQUEST, (JsonObject) o.toGson(null, gson));
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolAuthenticate message) {
    ProtocolAuthenticateGsonImpl o = new ProtocolAuthenticateGsonImpl(message);
    return new JsonMessageWrapper(seqNum, MSG_AUTH, (JsonObject) o.toGson(null, gson));
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolOpenRequest message) {
    ProtocolOpenRequestGsonImpl o = new ProtocolOpenRequestGsonImpl(message);
    return new JsonMessageWrapper(seqNum, MSG_OPEN_REQUEST, (JsonObject) o.toGson(null, gson));

  }

  @Override
  public String toJson(MessageWrapper messageWrapper) {
    return ((JsonMessageWrapper) messageWrapper).toJson();
  }

  @Override
  public ProtocolWaveletDelta createWaveletDelta(WaveletName wavelet, WaveletDelta delta) {

    ProtocolWaveletDeltaGsonImpl protocolDelta = new ProtocolWaveletDeltaGsonImpl();
    for (WaveletOperation op : delta) {
      protocolDelta.addOperation(JavaWaveletOperationSerializer.serialize(op));
    }
    protocolDelta.setAuthor(delta.getAuthor().getAddress());
    protocolDelta.setHashedVersion(versions.getServerVersion(wavelet, delta));
    return protocolDelta;

  }

  @Override
  public ProtocolSubmitRequest createSubmitRequest(WaveletName waveletName, WaveletDelta delta,
      String channelId) {
    ProtocolSubmitRequestGsonImpl o = new ProtocolSubmitRequestGsonImpl();
    o.setChannelId(channelId);
    o.setDelta(createWaveletDelta(waveletName, delta));
    o.setWaveletName(serialize(waveletName));
    return o;
  }

  @Override
  public ProtocolHashedVersion serialize(HashedVersion version) {
    return JavaWaveletOperationSerializer.serialize(version);
  }

  @Override
  public DocOp deserializeDocOp(Object rawJson) {
    ProtocolDocumentOperationGsonImpl protocolDocOp = new ProtocolDocumentOperationGsonImpl();
    try {
      protocolDocOp.fromGson((JsonElement) rawJson, gson, null);
      return WaveletOperationSerializer.deserialize(protocolDocOp);
    } catch (JsonSyntaxException e) {
      throw new IllegalStateException(e);
    } catch (GsonException e) {
      throw new IllegalStateException(e);
    }
  }

}
