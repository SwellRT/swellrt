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
import org.swellrt.beta.model.SHandler;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.remote.SNodeRemoteContainer;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.AbstractProfileManager;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;


@JsType(namespace = "swellrt", name = "Service")
public class ServiceFrontend implements ServiceBasis {
  
  public static final String ANONYMOUS_USER_ID  = "_anonymous_";

  public static final String STATUS_CONNECTED = WaveWebSocketClient.ConnectState.CONNECTED.toString();
  public static final String STATUS_DISCONNECTED = WaveWebSocketClient.ConnectState.DISCONNECTED.toString();
  public static final String STATUS_ERROR = WaveWebSocketClient.ConnectState.ERROR.toString();
  public static final String STATUS_CONNECTING = WaveWebSocketClient.ConnectState.CONNECTING.toString();


  @JsIgnore
  public static ServiceFrontend create(ServiceContext context) {
    Preconditions.checkNotNull(context, "Service context can't be null");
    ServiceFrontend sf = new ServiceFrontend(context);
    return sf;
  }
  
  private final ServiceContext context;
  
  private ProfileManager profileManager = new AbstractProfileManager() {

    @Override
    public String getCurrentSessionId() {
      return context.getSessionId() != null ? context.getSessionId() : "";
    }

    @Override
    public ParticipantId getCurrentParticipantId() {
      try {
        return ParticipantId.ofUnsafe(context.getParticipantId());
      } catch (Exception e) {
        return null;
      }
    }

    @Override
    protected void requestProfile(ParticipantId participantId, RequestProfileCallback callback) {
       // TODO complete request to service
    }
    
  };
  
  @JsIgnore
  protected ServiceFrontend(ServiceContext context) {
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
  
  @Override
  @JsProperty
  public ProfileManager getProfilesManager() {
    return profileManager;
  }
  
  @Override
  public void addConnectionHandler(ConnectionHandler h) {
    context.addConnectionHandler(h);
  }
  
  @Override
  public void removeConnectionHandler(ConnectionHandler h) {
    context.removeConnectionHandler(h);
  }
  
  @Override
  public void listen(Object object, SHandler handler) {
    
    if (handler == null)
      return;
    
   
    // As container
    SNodeRemoteContainer nodeContainer = SUtils.asContainer(object);
    
    if (nodeContainer != null)
      nodeContainer.listen(handler);
    
  }
  
}
