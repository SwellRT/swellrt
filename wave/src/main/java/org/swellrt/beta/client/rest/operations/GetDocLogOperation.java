package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.DocLogDelta;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Query doc log of changes.
 *
 */
public class GetDocLogOperation
    extends ServerOperation<GetDocLogOperation.Options, GetDocLogOperation.Response> {

  public static class Options implements ServerOperation.Options {

    public final WaveId waveId;
    public final WaveletId waveletId;
    public final String docId;
    public HashedVersion startHashVersion;
    public HashedVersion endHashVersion;
    public int numberOfResults;
    public boolean returnOps;
    public boolean orderDesc;

    public Options(WaveId waveId, WaveletId waveletId, String docId) {
      super();
      this.waveId = waveId;
      this.waveletId = waveletId;
      this.docId = docId;
    }


  }

  @JsType(isNative = true)
  public static interface Response extends ServerOperation.Response {

    @JsProperty
    DocLogDelta[] getLog();

  }

  public static class ResponseImpl implements GetDocLogOperation.Response {

    @Override
    public DocLogDelta[] getLog() {
      return new DocLogDelta[] {};
    }

  }

  public GetDocLogOperation(ServiceContext context, GetDocLogOperation.Options options,
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

    // Example
    // http://localhost:9898/rest/data/wave/local.net/demo-pad-list/wavelet/local.net/data+master/doc/t+cGsFYSqowZB/log?l=10&sort=des

    addPathElement("wave");
    addPathElement(ModernIdSerialiser.get().serialiseWaveId(options.waveId));
    addPathElement("wavelet");
    addPathElement(ModernIdSerialiser.get().serialiseWaveletId(options.waveletId));
    addPathElement("doc");
    addPathElement(options.docId);
    addPathElement("log");

    if (options.numberOfResults != 0)
      addQueryParam("l", options.numberOfResults + "");

    if (options.startHashVersion != null)
      addQueryParam("vs", options.startHashVersion.serialise());

    if (options.endHashVersion != null)
      addQueryParam("ve", options.endHashVersion.serialise());

    if (options.returnOps)
      addQueryParam("ops", "true");

    if (options.orderDesc)
      addQueryParam("sort", "des");

  }


}
