package org.swellrt.beta.client;

import org.swellrt.beta.client.js.SessionManagerJs;
import org.swellrt.beta.client.operation.OperationException;
import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.impl.CloseOperation;
import org.swellrt.beta.client.operation.impl.LoginOperation;
import org.swellrt.beta.client.operation.impl.LogoutOperation;
import org.swellrt.beta.client.operation.impl.OpenOperation;
import org.swellrt.beta.client.operation.impl.QueryOperation;
import org.swellrt.beta.client.operation.impl.ResumeOperation;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;


@JsType(namespace = "swellrt")
public class ServiceFrontend {


  @JsIgnore
  public static ServiceFrontend create() {
    ServiceFrontend sf = new ServiceFrontend();
    sf.setContext(new ServiceContext(SessionManagerJs.create()));    
    return sf;
  }
  
  private ServiceContext context;
  
  @JsIgnore
  protected ServiceFrontend() {
  }
  
  @JsIgnore
  protected void setContext(ServiceContext context) {
    this.context = context;  
  }
  
  public void login(LoginOperation.Options options, Callback<LoginOperation.Response> callback) {   
    LoginOperation op = new LoginOperation(context);
    op.execute(options, callback);    
  }
  
  public void logout(LogoutOperation.Options options, @JsOptional Callback<LogoutOperation.Response> callback) {
    LogoutOperation op = new LogoutOperation(context);
    op.execute(options, callback);    
  }
  
  public void resume(ResumeOperation.Options options, @JsOptional Callback<ResumeOperation.Response> callback) {
    ResumeOperation op = new ResumeOperation(context);
    op.execute(options, callback);    
  }
  
  public void open(OpenOperation.Options options, Callback<OpenOperation.Response> callback) throws OperationException {
    OpenOperation op = new OpenOperation(context);
    op.execute(options, callback);    
  }
  
  public void close(CloseOperation.Options options, @JsOptional Callback<CloseOperation.Response> callback) {
    CloseOperation op = new CloseOperation(context);
    op.execute(options, callback);    
  }
  
  public void query(QueryOperation.Options options, Callback<QueryOperation.Response> callback) {
    QueryOperation op = new QueryOperation(context);
    op.execute(options, callback);    
  }
  
}
