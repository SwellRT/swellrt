package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.wave.DiffData;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.version.HashedVersion;

import com.google.gwt.core.client.JavaScriptObject;

public final class GetDiffDataOperation
    extends ServerOperation<GetDiffDataOperation.Options, GetDiffDataOperation.Response> {

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

  public static final class Response extends JavaScriptObject
      implements ServerOperation.Response, DiffData.WaveletDiffData {

    protected Response() {

    }

    @Override
    public DiffData[] get(String blipId) {
      return (DiffData[]) JsoView.as(this).getObjectUnsafe(blipId);
    }

  }


  public GetDiffDataOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }


  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }

  @Override
  public String getRestContext() {
    return "contrib";
  }

  @Override
  public void buildRestParams() throws SException {
    addPathElement(getOptions().waveId);
    addPathElement(getOptions().waveletId);
    addPathElement(getOptions().base64HashVersion);
    addPathElement(String.valueOf(getOptions().version));
  }

}
