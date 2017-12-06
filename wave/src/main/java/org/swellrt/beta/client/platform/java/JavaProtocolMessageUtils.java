package org.swellrt.beta.client.platform.java;

import org.swellrt.beta.client.wave.ProtocolMessageUtils;
import org.waveprotocol.box.common.comms.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.ProtocolAuthenticationResult;
import org.waveprotocol.box.common.comms.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitResponse;
import org.waveprotocol.box.common.comms.ProtocolWaveletUpdate;
import org.waveprotocol.box.webclient.common.WaveletOperationSerializer;
import org.waveprotocol.wave.federation.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.gson.ProtocolWaveletDeltaGsonImpl;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

public class JavaProtocolMessageUtils extends ProtocolMessageUtils {

  @Override
  public MessageWrapper parseWrapper(String json) throws ParseException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ProtocolWaveletUpdate unwrapWaveletUpdate(MessageWrapper message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ProtocolSubmitResponse unwrapSubmitResponse(MessageWrapper message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ProtocolAuthenticationResult unwrapAuthResult(MessageWrapper message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ProtocolMessageUtils.RpcFinished unwrapRpcFinished(MessageWrapper message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolSubmitRequest message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolAuthenticate message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MessageWrapper wrap(int seqNum, ProtocolOpenRequest message) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String toJson(MessageWrapper messageWrapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ProtocolWaveletDelta createWaveletDelta(WaveletName wavelet, WaveletDelta delta) {

    ProtocolWaveletDeltaGsonImpl protocolDelta = new ProtocolWaveletDeltaGsonImpl();
    for (WaveletOperation op : delta) {
      protocolDelta.addOperation(WaveletOperationSerializer.serialize(op));
    }
    protocolDelta.setAuthor(delta.getAuthor().getAddress());
    protocolDelta.setHashedVersion(versions.getServerVersion(wavelet, delta));
    return protocolDelta;

  }

  @Override
  public ProtocolSubmitRequest createSubmitRequest(WaveletName waveletName, WaveletDelta delta,
      String channelId) {
    // TODO Auto-generated method stub
    return null;
  }

}
