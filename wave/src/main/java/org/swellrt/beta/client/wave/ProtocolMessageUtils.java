package org.swellrt.beta.client.wave;

import org.waveprotocol.box.common.comms.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.ProtocolAuthenticationResult;
import org.waveprotocol.box.common.comms.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitResponse;
import org.waveprotocol.box.common.comms.ProtocolWaveletUpdate;
import org.waveprotocol.box.webclient.common.WaveletOperationSerializer;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.federation.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

public abstract class ProtocolMessageUtils {

  public static final String MSG_WAVELET_UPDATE = "ProtocolWaveletUpdate";
  public static final String MSG_SUBMIT_RESPONSE = "ProtocolSubmitResponse";
  public static final String MSG_AUTH_RESULT = "ProtocolAuthenticationResult";
  public static final String MSG_RPC_FINISHED = "RpcFinished";

  public static final String MSG_AUTH = "ProtocolAuthenticate";
  public static final String MSG_SUBMIT_REQUEST = "ProtocolSubmitRequest";
  public static final String MSG_OPEN_REQUEST = "ProtocolOpenRequest";

  public static interface MessageWrapper {

    // sequenceNumber
    int getSequenceNumber();

    // messageType
    String getMessageType();

    Object getMessage();

    boolean isProtocolWaveletUpdate();

    boolean isProtocolSubmitResponse();

    boolean isProtocolAuthenticationResult();

    boolean isRpcFinished();

  }


  @SuppressWarnings("serial")
  public static class ParseException extends Exception {

  }

  /**
   * Hand made wrapper class for messages of type RpcFinished. This is a
   * workaround, because real implementation of this protobuf wrapper is not
   * visible for client.
   */
  public static interface RpcFinished {

    public boolean hasFailed();

    public boolean hasErrorText();

    public String getErrorText();

    public ChannelException getChannelException();

  }

  protected final VersionSignatureManager versions = WaveFactories.versionSignatureManager;

  public abstract MessageWrapper parseWrapper(String json) throws ParseException;

  public abstract ProtocolWaveletUpdate unwrapWaveletUpdate(MessageWrapper message);

  public abstract ProtocolSubmitResponse unwrapSubmitResponse(MessageWrapper message);

  public abstract ProtocolAuthenticationResult unwrapAuthResult(MessageWrapper message);

  public abstract ProtocolMessageUtils.RpcFinished unwrapRpcFinished(MessageWrapper message);


  public abstract MessageWrapper wrap(int seqNum, ProtocolSubmitRequest message);

  public abstract MessageWrapper wrap(int seqNum, ProtocolAuthenticate message);

  public abstract MessageWrapper wrap(int seqNum, ProtocolOpenRequest message);

  public abstract ProtocolWaveletDelta createWaveletDelta(WaveletName wavelet, WaveletDelta delta);

  public abstract ProtocolSubmitRequest createSubmitRequest(WaveletName waveletName,
      WaveletDelta delta, String channelId);

  public abstract String toJson(MessageWrapper messageWrapper);

  public String serialize(WaveletName waveletName) {
    return RemoteViewServiceMultiplexer.serialize(waveletName);
  }

  public ProtocolHashedVersion serialize(HashedVersion version) {
    return WaveletOperationSerializer.serialize(version);
  }


}
