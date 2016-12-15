package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.client.operation.OperationException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class EchoOperation extends HTTPOperation<EchoOperation.Options, EchoOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {
    
  }
  
  @JsType(isNative = true)
  public interface Response extends Operation.Response {
    
    @JsProperty(name = "sessionCookie")
    public boolean isSessionCookie();
    
  }

  
  public EchoOperation(ServiceContext context) {
    super(context);    
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null)
      callback.onError(new OperationException(OperationException.OPERATION_EXCEPTION, exception.getMessage()));
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {
    Response response = generateResponse(data);
    getServiceContext().setSessionCookieAvailability(response.isSessionCookie());
    if (callback != null)
      callback.onSuccess(response);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {
    setSessionInURLFlag(false);
    addPathElement("echo");
    executeGet(callback);
  }
  
  
  
}
