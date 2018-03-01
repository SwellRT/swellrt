package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.wave.DiffData;
import org.waveprotocol.wave.client.wave.DiffDataImpl;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.version.HashedVersion;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class GetContributionsOperation
    extends ServerOperation<GetContributionsOperation.Options, GetContributionsOperation.Response> {

  public static class Options implements ServerOperation.Options {

    public String waveId;
    public String waveletId;
    public String blipId;
    public String base64HashVersion;
    public long version;

    public Options(String waveId, String waveletId, String blipId, HashedVersion hashedVersion) {
      super();
      this.waveId = waveId;
      this.waveletId = waveletId;
      this.blipId = blipId;
      this.base64HashVersion = CharBase64.encodeWebSafe(hashedVersion.getHistoryHash(), true);
      this.version = hashedVersion.getVersion();
    }

  }

  @JsType(isNative = true)
  public static interface Response
      extends ServerOperation.Response {

    @JsProperty
    public <R extends DiffData<?, ?>> R[] getContrib();

  }


  public static class ResponseImpl implements Response {

    public DiffDataImpl[] contrib;

    @SuppressWarnings("unchecked")
    @Override
    public DiffDataImpl[] getContrib() {
      return contrib;
    }

  }

  public GetContributionsOperation(ServiceContext context, GetContributionsOperation.Options options,
      ServiceOperation.Callback<GetContributionsOperation.Response> callback) {
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
    addPathElement(options.waveId);
    addPathElement("wavelet");
    addPathElement(options.waveletId);
    addPathElement("contrib");
    addQueryParam("version", options.version + ":" + options.base64HashVersion);
  }

}
