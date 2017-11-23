package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class PasswordOperation extends HTTPOperation<PasswordOperation.Options, PasswordOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();

    @JsProperty
    public String getOldPassword();

    @JsProperty
    public String getNewPassword();

    @JsProperty
    public String getToken();

  }

  public static final class Response extends JavaScriptObject implements Operation.Response {

    protected Response() {

    }

  }


  public PasswordOperation(ServiceContext context) {
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


    addPathElement("password");
    addQueryParam("id", options.getId());

    if (options.getOldPassword() != null) {
      addQueryParam("token-or-password", options.getOldPassword());
    } else if (options.getToken() != null) {
      addQueryParam("token-or-password", options.getToken());
    } else {
      callback.onError(new SException(SException.MISSING_PARAMETERS, null,
          "Missing token or old password parameter"));
    }

    addQueryParam("new-password", options.getNewPassword());

    executePost(callback);
  }



}
