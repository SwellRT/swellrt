package org.swellrt.beta.client.rest.operations;

import java.util.Collections;
import java.util.Map;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.ResponseWrapper;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * Get doc snapshot
 *
 */
public class GetDocSnapshotOperation
    extends ServerOperation<GetDocSnapshotOperation.Options, ResponseWrapper> {

  public static class Options implements ServerOperation.Options {

    public final WaveId waveId;
    public final WaveletId waveletId;
    public final String docId;
    public HashedVersion version;

    public Options(WaveId waveId, WaveletId waveletId, String docId) {
      super();
      this.waveId = waveId;
      this.waveletId = waveletId;
      this.docId = docId;
    }

  }

  public GetDocSnapshotOperation(ServiceContext context, GetDocSnapshotOperation.Options options,
      ServiceOperation.Callback<ResponseWrapper> callback) {
    super(context, options, callback);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }

  @Override
  public String getRestContext() {
    return "/rest/data";
  }


  @Override
  protected void doSuccessRaw(String raw) {
    ResponseWrapper wrapper = new ResponseWrapper();
    wrapper.setResponse(raw);
    doSuccess(wrapper);
  }

  @Override
  protected Map<String, String> getHeaders() {
    return Collections.singletonMap("Accept", "text/plain, application/xml");
  }

  @Override
  protected void buildRestParams() throws SException {

    // Example
    // http://localhost:9898/rest/data/wave/local.net/demo-pad-list/wavelet/local.net/data+master/doc/t+cGsFYSqowZB/content?v=...

    addPathElement("wave");
    addPathElement(ModernIdSerialiser.get().serialiseWaveId(options.waveId));
    addPathElement("wavelet");
    addPathElement(ModernIdSerialiser.get().serialiseWaveletId(options.waveletId));
    addPathElement("doc");
    addPathElement(options.docId);
    addPathElement("content");

    if (options.version != null)
      addQueryParam("v", options.version.serialise());


  }


}
