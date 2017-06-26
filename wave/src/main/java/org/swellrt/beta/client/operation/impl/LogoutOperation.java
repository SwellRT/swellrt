package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.common.SwellUtils;

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
    if (callback != null)
      callback.onSuccess(new Response(){
      });
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    getServiceContext().reset();

    addPathElement("auth");
    if (options.getId() != null)
      addPathElement(
          SwellUtils.addDomainToParticipant(options.getId(), getServiceContext().getWaveDomain()));

    executeDelete(callback);
  }



}
