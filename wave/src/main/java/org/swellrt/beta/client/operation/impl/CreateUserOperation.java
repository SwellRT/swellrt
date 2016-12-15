package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.client.operation.OperationException;
import org.swellrt.beta.client.operation.data.ProfileData;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class CreateUserOperation extends HTTPOperation<CreateUserOperation.Options, CreateUserOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {
    
    @JsProperty
    public String getId();

    @JsProperty
    public String getPassword();

    @JsProperty
    public String getEmail();
    
    @JsProperty
    public String getLocale();
    
    @JsProperty
    public String getName();

    @JsProperty
    public String getAvatarData();


  }
  
  @JsType(isNative = true)
  public interface Response extends Operation.Response, ProfileData {
        
  }

  
  public CreateUserOperation(ServiceContext context) {
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
    if (callback != null)
      callback.onSuccess(response);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {
    
    if (options == null || 
        options.getId() == null ||
        options.getPassword() == null) {
    
      if (callback != null)
    	  callback.onError(new OperationException(OperationException.MISSING_PARAMETERS,""));
    }
    
    addPathElement("account");   
    setBody(generateBody(options));
    executePost(callback);
  }
  
  
  
}
