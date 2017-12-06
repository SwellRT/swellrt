package org.swellrt.beta.client.platform.js;

import org.swellrt.beta.client.wave.ProtocolMessageUtils;
import org.waveprotocol.box.common.comms.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.ProtocolAuthenticationResult;
import org.waveprotocol.box.common.comms.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitResponse;
import org.waveprotocol.box.common.comms.ProtocolWaveletUpdate;
import org.waveprotocol.box.common.comms.jso.ProtocolAuthenticateJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolOpenRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitResponseJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolWaveletUpdateJsoImpl;
import org.waveprotocol.box.webclient.common.WaveletOperationSerializer;
import org.waveprotocol.wave.communication.gwt.JsonHelper;
import org.waveprotocol.wave.communication.gwt.JsonMessage;
import org.waveprotocol.wave.communication.json.JsonException;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.federation.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.jso.ProtocolWaveletDeltaJsoImpl;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

import com.google.gwt.core.client.JavaScriptObject;

public class JsProtocolMessageUtils extends ProtocolMessageUtils {

  public static final class JsonMessageWrapper extends JsonMessage implements MessageWrapper {

    protected JsonMessageWrapper() {

    }

    static MessageWrapper create(int seqno, String type, JavaScriptObject jsoMessage) {
      JsonMessageWrapper wrapper = JsonMessage.createJsonMessage().cast();
      JsonHelper.setPropertyAsInteger(wrapper, "sequenceNumber", seqno);
      JsonHelper.setPropertyAsString(wrapper, "messageType", type);
      JsonHelper.setPropertyAsObject(wrapper, "message", jsoMessage);
      return wrapper;
    }

    @Override
    public int getSequenceNumber() {
      return JsonHelper.getPropertyAsInteger(this, "sequenceNumber");
    }

    @Override
    public String getMessageType() {
      return JsonHelper.getPropertyAsString(this, "messageType");
    }

    @Override
    public Object getMessage() {
      return JsonHelper.getPropertyAsObject(this, "message").cast();
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

  public static final class JsonRpcFinished extends JsonMessage implements RpcFinished {

    protected JsonRpcFinished() {
      super();
    }

    public boolean hasFailed() {
      return JsonHelper.getPropertyAsBoolean(this, "1");
    }

    public boolean hasErrorText() {
      return JsonHelper.hasProperty(this, "2");
    }

    public String getErrorText() {
      return JsonHelper.getPropertyAsString(this, "2");
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

    try {
      return (JsonMessageWrapper) JsonMessage.parse(json);
    } catch (JsonException e) {
      throw new ParseException();
    }
  }


  @Override
  public ProtocolWaveletUpdate unwrapWaveletUpdate(MessageWrapper message) {
    ProtocolWaveletUpdateJsoImpl o = (ProtocolWaveletUpdateJsoImpl) message.getMessage();
    return o;
  }

  @Override
  public ProtocolSubmitResponse unwrapSubmitResponse(MessageWrapper message) {
    ProtocolSubmitResponseJsoImpl o = (ProtocolSubmitResponseJsoImpl) message.getMessage();
    return o;
  }

  @Override
  public ProtocolAuthenticationResult unwrapAuthResult(MessageWrapper message) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public RpcFinished unwrapRpcFinished(MessageWrapper message) {
    JsonRpcFinished o = (JsonRpcFinished) message.getMessage();
    return o;
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolSubmitRequest message) {
    ProtocolSubmitRequestJsoImpl jsoMessage = ProtocolSubmitRequestJsoImpl.create();
    jsoMessage.setChannelId(message.getChannelId());
    jsoMessage.setDelta(serialize(message.getDelta()));
    jsoMessage.setWaveletName(message.getWaveletName());
    return JsonMessageWrapper.create(seqNum, MSG_SUBMIT_REQUEST, jsoMessage);
  }

  protected ProtocolWaveletDeltaJsoImpl serialize(ProtocolWaveletDelta waveletDelta) {
    ProtocolWaveletDeltaJsoImpl jsoDelta = ProtocolWaveletDeltaJsoImpl.create();
    jsoDelta.setAuthor(waveletDelta.getAuthor());
    jsoDelta.setHashedVersion(waveletDelta.getHashedVersion());
    jsoDelta.addAllAddressPath(waveletDelta.getAddressPath());
    jsoDelta.addAllOperation(waveletDelta.getOperation());
    return jsoDelta;
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolAuthenticate message) {
    ProtocolAuthenticateJsoImpl jsoMessage = ProtocolAuthenticateJsoImpl.create();
    jsoMessage.setToken(message.getToken());
    return JsonMessageWrapper.create(seqNum, MSG_AUTH, jsoMessage);
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolOpenRequest message) {
    ProtocolOpenRequestJsoImpl jsoMessage = ProtocolOpenRequestJsoImpl.create();
    jsoMessage.setParticipantId(message.getParticipantId());
    jsoMessage.setWaveId(message.getWaveId());

    jsoMessage.addAllKnownWavelet(message.getKnownWavelet());
    jsoMessage.addAllWaveletIdPrefix(message.getWaveletIdPrefix());

    return JsonMessageWrapper.create(seqNum, MSG_OPEN_REQUEST, jsoMessage);
  }

  @Override
  public ProtocolSubmitRequest createSubmitRequest(WaveletName waveletName, WaveletDelta delta,
      String channelId) {

    ProtocolSubmitRequestJsoImpl submitRequest = ProtocolSubmitRequestJsoImpl.create();
    submitRequest.setWaveletName(serialize(waveletName));
    submitRequest.setDelta(createWaveletDelta(waveletName, delta));
    submitRequest.setChannelId(channelId);

    return submitRequest;
  }

  public ProtocolWaveletDelta createWaveletDelta(WaveletName wavelet, WaveletDelta delta) {

    ProtocolWaveletDeltaJsoImpl protocolDelta = ProtocolWaveletDeltaJsoImpl.create();
    for (WaveletOperation op : delta) {
      protocolDelta.addOperation(WaveletOperationSerializer.serialize(op));
    }
    protocolDelta.setAuthor(delta.getAuthor().getAddress());
    protocolDelta.setHashedVersion(versions.getServerVersion(wavelet, delta));
    return protocolDelta;

  }


  @Override
  public String toJson(MessageWrapper messageWrapper) {
    JsonMessageWrapper jsoWrapper = (JsonMessageWrapper) messageWrapper;
    return jsoWrapper.toJson();
  }

}
