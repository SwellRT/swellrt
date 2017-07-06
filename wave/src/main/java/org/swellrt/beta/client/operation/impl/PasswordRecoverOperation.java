package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class PasswordRecoverOperation extends HTTPOperation<PasswordRecoverOperation.Options, PasswordRecoverOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getEmail();

    @JsProperty
    public String getUrl();

  }

  public static final class Response extends JavaScriptObject implements Operation.Response {

    protected Response() {

    }

  }


  public PasswordRecoverOperation(ServiceContext context) {
    super(context);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null)
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {
    if (callback != null)
      callback.onSuccess(null);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    addPathElement("email");

    addQueryParam("email", options.getEmail());
    addQueryParam("recover-url", options.getUrl());
    addQueryParam("method", "password-reset");

    executePost(callback);
  }



}
