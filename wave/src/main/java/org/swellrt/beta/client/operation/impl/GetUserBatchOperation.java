package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class GetUserBatchOperation extends HTTPOperation<GetUserBatchOperation.Options, GetUserBatchOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String[] getId();


  }

  @JsType(isNative = true)
  public interface Response extends Operation.Response {

  }


  public GetUserBatchOperation(ServiceContext context) {
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

    if (options == null ||
        options.getId() == null) {

      if (callback != null)
    	  callback.onError(new SException(SException.MISSING_PARAMETERS));
    }
    String userIdQuery = "";
    if (options.getId() != null) {
      for (int i = 0; i < options.getId().length; i++) {
        if (!userIdQuery.isEmpty())
          userIdQuery += ";";
        userIdQuery += options.getId()[i];
      }
    }
    addQueryParam("p", userIdQuery);

    addPathElement("account");
    executeGet(callback);
  }



}
