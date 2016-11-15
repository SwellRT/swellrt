package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.Operation.Options;
import org.swellrt.beta.client.operation.Operation.Response;

import jsinterop.annotations.JsType;

public final class CloseOperation implements Operation<CloseOperation.Options, CloseOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {
    
    public String getId();
  }
  
  @JsType
  public interface Response extends Operation.Response {
    
    // TODO change object to right type
    public Object getObject(); 
    
  }
  
  private final ServiceContext context;
  
  public CloseOperation(ServiceContext context) {
    this.context = context;
  }
  

  @Override
  public void execute(Options options, Callback<Response> callback) {     
   
  }

  
  
}
