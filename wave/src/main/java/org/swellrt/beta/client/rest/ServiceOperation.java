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
  public interface Callback<T extends ServiceOperation.Response>
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
    if (callback != null) {

      if (exception instanceof IllegalArgumentException) {

        callback.onError(new SException(SException.INVALID_PARAMETERS, exception.getCause(),
            "Not valid parameters"));

      }
      if (exception instanceof SException) {

        callback.onError((SException) exception);

      } else {

        callback.onError(new SException(SException.OPERATION_EXCEPTION, exception,
            "Error executing service operation."));

      }

    }

  }

  protected void validateOptions() {

  }

}
