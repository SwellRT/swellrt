package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.content.DocContributionsFetcher;
import org.waveprotocol.wave.client.editor.content.DocContributionsFetcher.DocContribution;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsType;

public final class FetchContributionsOperation extends HTTPOperation<FetchContributionsOperation.Options, FetchContributionsOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    public String getWaveId();
    public String getWaveletId();
    public String getBlipId();
    public String getBase64HashVersion();
    public double getLongVersion();


  }

  public static final class Response extends JavaScriptObject implements Operation.Response, DocContributionsFetcher.WaveletContributions {

    protected Response() {

    }

    @Override
    public DocContribution[] getDocContributions(String blipId) {
      return (DocContribution[]) JsoView.as(this).getObjectUnsafe(blipId);
    }

  }


  public FetchContributionsOperation(ServiceContext context) {
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
    addPathElement(options.getWaveId());
    addPathElement(options.getWaveletId());
    addPathElement(options.getBlipId());
    addPathElement(options.getBase64HashVersion());
    addPathElement(String.valueOf(options.getLongVersion()));
    executeGet(callback);
  }



}
