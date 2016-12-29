package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.client.operation.data.ProfileData;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class ResumeOperation extends HTTPOperation<ResumeOperation.Options, ResumeOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();
        
  }
  
  @JsType(isNative = true)
  public interface Response extends Operation.Response, ProfileData {
    

  }

  
  public ResumeOperation(ServiceContext context) {
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
    getServiceContext().init(response);
    
    if (callback != null)
      callback.onSuccess(response);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {
    addPathElement("auth");
    if (options.getId() != null)
      addPathElement(options.getId());
      
    executeGet(callback);
  }
  
  
  
}
