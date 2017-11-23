package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsType;

/**
 * Log in operation
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public final class ListLoginOperation extends HTTPOperation<ListLoginOperation.Options, ListLoginOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

  }

  @JsType(isNative = true)
  public interface Response extends Operation.Response {
    // TODO map JSON response to Java properties
  }

  public ListLoginOperation(ServiceContext context) {
    super(context);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {
    addPathElement("auth");
    executeGet(callback);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null) {
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
    }
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {
    if (callback != null) {
      callback.onSuccess(generateResponse(data));
    }
  }


}
