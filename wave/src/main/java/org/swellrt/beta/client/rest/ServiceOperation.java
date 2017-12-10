package org.swellrt.beta.client.rest;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType()
public abstract class ServiceOperation<O extends ServiceOperation.Options, R extends ServiceOperation.Response> {

  @JsType(isNative = true)
  public static interface Options {

  }

  @JsType(isNative = true)
  public static interface Response {

  }

  @JsType(isNative = true)
  public static interface OperationError {

    @JsProperty
    public String getError();
  }

  @JsType(isNative = true)
  public static interface Callback<T extends ServiceOperation.Response>
  {

    public void onError(SException exception);

    public void onSuccess(T response);

  }

  protected final ServiceContext context;
  protected final O options;
  protected final Callback<R> callback;

  public ServiceOperation(ServiceContext context, O options, Callback<R> callback) {
    this.context = context;
    this.options = options;
    this.callback = callback;
  }

  protected void doSuccess(R response) {
    if (callback != null)
      callback.onSuccess(response);
  }

  protected void doFailure(Throwable exception) {
    if (callback != null)
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception,
          "Error executing service operation."));
  }

}
