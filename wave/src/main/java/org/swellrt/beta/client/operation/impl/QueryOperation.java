package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.Operation.Options;
import org.swellrt.beta.client.operation.Operation.Response;

import jsinterop.annotations.JsType;

public final class QueryOperation implements Operation<QueryOperation.Options, QueryOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {
    
    public String getQuery();
    public String getProjection();
    public String getAggregate();
    
  }
  
  @JsType
  public interface Response extends Operation.Response {
        
  }
  
  private final ServiceContext context;
  
  public QueryOperation(ServiceContext context) {
    this.context = context;
  }
  

  @Override
  public void execute(Options options, Callback<Response> callback) {     
   
  }

  
  
}
