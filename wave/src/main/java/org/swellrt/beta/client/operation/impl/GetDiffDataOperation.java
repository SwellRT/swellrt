package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.wave.DiffData;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.version.HashedVersion;

import com.google.gwt.core.client.JavaScriptObject;

public final class GetDiffDataOperation extends HTTPOperation<GetDiffDataOperation.Options, GetDiffDataOperation.Response> {

  public static class Options implements Operation.Options {

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
      implements Operation.Response, DiffData.WaveletDiffData {

    protected Response() {

    }

    @Override
    public DiffData[] get(String blipId) {
      return (DiffData[]) JsoView.as(this).getObjectUnsafe(blipId);
    }

  }


  public GetDiffDataOperation(ServiceContext context) {
    super(context);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null)
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {
    Response response = generateResponse(data);
    if (callback != null)
      callback.onSuccess(response);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    setPathContext("contrib");
    addPathElement(options.waveId);
    addPathElement(options.waveletId);
    addPathElement(options.base64HashVersion);
    addPathElement(String.valueOf(options.version));
    executeGet(callback);
  }



}
