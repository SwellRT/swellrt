package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.UriUtils;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class PasswordRecoverOperation extends HTTPOperation<PasswordRecoverOperation.Options, PasswordRecoverOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();

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

    if (options.getId() != null)
      addQueryParam("id-or-email", options.getId());
    else if (options.getEmail() != null)
      addQueryParam("id-or-email", UriUtils.encode(options.getEmail()));
    else if (callback != null) {
      callback.onError(
          new SException(SException.MISSING_PARAMETERS, null, "User id or email is required"));

      return;
    }

    addQueryParam("recover-url", options.getUrl());
    addQueryParam("method", "password-reset");

    executePost(callback);
  }



}
