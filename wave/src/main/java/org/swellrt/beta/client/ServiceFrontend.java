package org.swellrt.beta.client;

import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.impl.CloseOperation;
import org.swellrt.beta.client.operation.impl.CreateUserOperation;
import org.swellrt.beta.client.operation.impl.LoginOperation;
import org.swellrt.beta.client.operation.impl.LogoutOperation;
import org.swellrt.beta.client.operation.impl.OpenOperation;
import org.swellrt.beta.client.operation.impl.QueryOperation;
import org.swellrt.beta.client.operation.impl.ResumeOperation;
import org.swellrt.beta.client.wave.WaveWebSocketClient;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SHandler;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.remote.SNodeRemoteContainer;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;


@JsType(namespace = "swellrt")
public class ServiceFrontend {
  
  public static final String ANONYMOUS_USER_ID  = "_anonymous_";

  public static final String STATE_CONNECTED = WaveWebSocketClient.ConnectState.CONNECTED.toString();
  
  /**
   * 
   */
  @JsFunction
  public interface ConnectionHandler {
    void exec(String state, SException e);
  }

  @JsIgnore
  public static ServiceFrontend create(ServiceContext context) {
    ServiceFrontend sf = new ServiceFrontend();
    sf.setContext(context);    
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
  
  public void createUser(CreateUserOperation.Options options, Callback<CreateUserOperation.Response> callback) {
    CreateUserOperation op = new CreateUserOperation(context);
    op.execute(options, callback);
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
  
  public void open(OpenOperation.Options options, Callback<OpenOperation.Response> callback) {
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
  
  public void addConnectionHandler(ConnectionHandler h) {
    context.addConnectionHandler(h);
  }
  
  public void removeConnectionHandler(ConnectionHandler h) {
    context.removeConnectionHandler(h);
  }
  
  public void listen(Object object, SHandler handler) {
    
    if (handler == null)
      return;
    
   
    // As container
    SNodeRemoteContainer nodeContainer = SUtils.asContainer(object);
    
    if (nodeContainer != null)
      nodeContainer.listen(handler);
    
  }
  
}
