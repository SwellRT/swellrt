package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class LogoutOperation extends HTTPOperation<LogoutOperation.Options, LogoutOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();

  }

  @JsType(isNative = true)
  public interface Response extends Operation.Response {


  }

  private boolean resetContext = false;

  public LogoutOperation(ServiceContext context) {
    super(context);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null)
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
  }

  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {

    // reset the context after http call to send cookies
    if (resetContext)
      getServiceContext().reset();

    if (callback != null)
      callback.onSuccess(new Response(){
      });
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    addPathElement("auth");

    if (getServiceContext().isSession()) {
      if (options == null || options.getId() == null ||
            (options.getId() != null && options.getId().equals(getServiceContext().getParticipantId()))) {
        addPathElement(getServiceContext().getParticipantId());
        resetContext = true;
        }
    } else if (options.getId() != null) {
      addPathElement(options.getId());
    } else {
      callback.onError(new SException(1, null, "Missing user parameter"));
      return;
    }

    executeDelete(callback);
  }



}
