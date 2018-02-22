package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.LogDocRevision;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Query document's log of changes as revisions.
 *
 */
public class GetDocRevisionOperation
    extends ServerOperation<GetDocRevisionOperation.Options, GetDocRevisionOperation.Response> {

  public static class Options implements ServerOperation.Options {

    public final WaveId waveId;
    public final WaveletId waveletId;
    public final String docId;
    public HashedVersion startHashVersion;
    public int numberOfResults;

    public Options(WaveId waveId, WaveletId waveletId, String docId, HashedVersion startVersion,
        int numOfResults) {
      super();
      this.waveId = waveId;
      this.waveletId = waveletId;
      this.docId = docId;
      this.startHashVersion = startVersion;
      this.numberOfResults = numOfResults;
    }


  }

  @JsType(isNative = true)
  public static interface Response extends ServerOperation.Response {

    @JsProperty
    LogDocRevision[] getLog();

  }

  public static class ResponseImpl implements GetDocRevisionOperation.Response {

    @Override
    public LogDocRevision[] getLog() {
      return new LogDocRevision[] {};
    }

  }

  public GetDocRevisionOperation(ServiceContext context, GetDocRevisionOperation.Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback, ResponseImpl.class);
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
  protected void buildRestParams() throws SException {

    addPathElement("wave");
    addPathElement(ModernIdSerialiser.get().serialiseWaveId(options.waveId));
    addPathElement("wavelet");
    addPathElement(ModernIdSerialiser.get().serialiseWaveletId(options.waveletId));
    addPathElement("doc");
    addPathElement(options.docId);
    addPathElement("log");


    addQueryParam("results", options.numberOfResults + "");
    addQueryParam("vend", options.startHashVersion.serialise());
    addQueryParam("withops", "true");
    addQueryParam("sort", "des");
    addQueryParam("groupbyuser", "true");

  }


}
