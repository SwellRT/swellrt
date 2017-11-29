package org.swellrt.beta.client.rest;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public abstract class ServiceOperation<O extends ServiceOperation.Options, R extends ServiceOperation.Response> {

  @JsType(isNative = true)
  public interface Options {

  }

  @JsType(isNative = true)
  public interface Response {

  }

  @JsType(isNative = true)
  public interface OperationError {

    @JsProperty
    public String getError();
  }

  @JsType(isNative = true)
  public interface Callback<T extends Response> {

    public void onError(SException exception);

    public void onSuccess(T response);

  }

  private final ServiceContext context;
  private final O options;
  private final Callback<R> callback;


  public ServiceOperation(ServiceContext context, O options, Callback<R> callback) {
    this.context = context;
    this.options = options;
    this.callback = callback;
  }


  public O getOptions() {
    return options;
  }

  public void doSuccess(R response) {
    if (callback != null)
      callback.onSuccess(response);
  }

  public void doFailure(Throwable exception) {
    if (callback != null)
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception,
          "Error executing service operation."));
  }

  protected ServiceContext getContext() {
    return context;
  }
}
