package org.swellrt.beta.common;

import jsinterop.annotations.JsType;

/**
 * Interface for async operations. Each instance is a
 * single execution of the operation that could be run in a different
 * thread from the operation client.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 * @param <O> operation options
 * @param <R> operation callback
 */
public interface Operation<O extends Operation.Options, R extends Operation.Response> {

  @SuppressWarnings("serial")
  class OperationException extends Exception {

    private final int statusCode;
    private final String statusMessage;

    public int getStatusCode() {
      return statusCode;
    }

    public String getStatusMessage() {
      return statusMessage;
    }

    public OperationException(int statusCode, String statusMessage) {
      this.statusCode = statusCode;
      this.statusMessage = statusMessage;
    }

  }

  @JsType(isNative = true)
  public interface Options {

  }

  @JsType(isNative = true)
  public interface Response {

  }

  @JsType(isNative = true)
  public interface Callback<T extends Response> {

    public void onError(SException exception);

    public void onSuccess(T response);

  }


  public void execute(O options, Callback<R> callback);

}
