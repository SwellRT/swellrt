package org.swellrt.beta.client;


import org.swellrt.beta.client.rest.ClientOperationExecutor;
import org.swellrt.beta.client.rest.ServerOperationExecutor;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.CloseOperation;
import org.swellrt.beta.client.rest.operations.CreateUserOperation;
import org.swellrt.beta.client.rest.operations.EditUserOperation;
import org.swellrt.beta.client.rest.operations.GetUserBatchOperation;
import org.swellrt.beta.client.rest.operations.GetUserOperation;
import org.swellrt.beta.client.rest.operations.GetUserOperation.Response;
import org.swellrt.beta.client.rest.operations.ListLoginOperation;
import org.swellrt.beta.client.rest.operations.LoginOperation;
import org.swellrt.beta.client.rest.operations.LogoutOperation;
import org.swellrt.beta.client.rest.operations.OpenOperation;
import org.swellrt.beta.client.rest.operations.PasswordOperation;
import org.swellrt.beta.client.rest.operations.PasswordRecoverOperation;
import org.swellrt.beta.client.rest.operations.QueryOperation;
import org.swellrt.beta.client.rest.operations.ResumeOperation;
import org.swellrt.beta.client.rest.operations.naming.DeleteNameOperation;
import org.swellrt.beta.client.rest.operations.naming.GetNamesOperation;
import org.swellrt.beta.client.rest.operations.naming.SetNameOperation;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SObject;
import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.AbstractProfileManager;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;


@JsType(namespace = "swell", name = "DefaultService")
public class DefaultFrontend implements ServiceFrontend {



  @JsIgnore
  public static DefaultFrontend create(ServiceContext context,
      ServerOperationExecutor serverOpExecutor,
      ServiceLogger logger) {

    Preconditions.checkNotNull(context, "Service context can't be null");
    Preconditions.checkNotNull(serverOpExecutor, "Operation executor can't be null");
    Preconditions.checkNotNull(logger, "Service logger can't be null");

    DefaultFrontend sf = new DefaultFrontend(context, serverOpExecutor, logger);
    return sf;
  }

  private final ServiceContext context;
  private final ServerOperationExecutor serverOpExecutor;
  private final ClientOperationExecutor clientOpExecutor;
  private final ServiceLogger logger;

  private ProfileManager profileManager = new AbstractProfileManager() {

    @Override
    public String getCurrentSessionId() {
      if (context.getSessionId() != null) {
        String sessionToken = context.getSessionId()+":"+context.getTransientSessionId()+":"+context.getWindowId();
        return sessionToken;
      }
      return "";
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

      GetUserOperation.Options options = new GetUserOperation.Options() {

        @Override
        public String getId() {
          return participantId.getAddress();
        }

      };

      GetUserOperation op = new GetUserOperation(context, options,
          new ServiceOperation.Callback<GetUserOperation.Response>() {

            @Override
            public void onError(SException exception) {
              logger.log("Error loading profile " + exception.getMessage());
            }

            @Override
            public void onSuccess(Response response) {
              callback.onCompleted(response);
            }

          });

      serverOpExecutor.execute(op);

    }

    @Override
    protected void storeProfile(Profile profile) {

      EditUserOperation.Options options = new EditUserOperation.DefaultOptions(profile.getAddress(),
          profile.getName());

      EditUserOperation op = new EditUserOperation(context, options,
          new Callback<EditUserOperation.Response>() {

            @Override
            public void onError(SException exception) {
              logger.log("Error storing profile " + exception.getMessage());
            }

            @Override
            public void onSuccess(
                EditUserOperation.Response response) {
            }

          });

      serverOpExecutor.execute(op);
    }

  };

  @JsIgnore
  protected DefaultFrontend(ServiceContext context,
      ServerOperationExecutor serverOpExecutor,
      ServiceLogger logger) {
    this.context = context;
    this.serverOpExecutor = serverOpExecutor;
    this.clientOpExecutor = new ClientOperationExecutor();
    this.logger = logger;
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#createUser(org.swellrt.beta.client.rest.operations.CreateUserOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void createUser(CreateUserOperation.Options options, Callback<CreateUserOperation.Response> callback) {
    CreateUserOperation op = new CreateUserOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#login(org.swellrt.beta.client.rest.operations.LoginOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void login(LoginOperation.Options options, Callback<LoginOperation.Response> callback) {
    LoginOperation op = new LoginOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#logout(org.swellrt.beta.client.rest.operations.LogoutOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void logout(LogoutOperation.Options options, @JsOptional Callback<LogoutOperation.Response> callback) {
    LogoutOperation op = new LogoutOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#resume(org.swellrt.beta.client.rest.operations.ResumeOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void resume(ResumeOperation.Options options, @JsOptional Callback<ResumeOperation.Response> callback) {
    ResumeOperation op = new ResumeOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#open(org.swellrt.beta.client.rest.operations.OpenOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void open(OpenOperation.Options options, Callback<SObject> callback) {
    OpenOperation op = new OpenOperation(context, options, callback);
    clientOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#close(org.swellrt.beta.client.rest.operations.CloseOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void close(CloseOperation.Options options, @JsOptional Callback<CloseOperation.Response> callback) {
    CloseOperation op = new CloseOperation(context, options, callback);
    clientOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#query(org.swellrt.beta.client.rest.operations.QueryOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void query(QueryOperation.Options options, Callback<QueryOperation.Response> callback) {
    QueryOperation op = new QueryOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#getUser(org.swellrt.beta.client.rest.operations.GetUserOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void getUser(GetUserOperation.Options options, Callback<GetUserOperation.Response> callback) {
    GetUserOperation op = new GetUserOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#getUserBatch(org.swellrt.beta.client.rest.operations.GetUserBatchOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void getUserBatch(GetUserBatchOperation.Options options,
      Callback<GetUserBatchOperation.Response> callback) {
    GetUserBatchOperation op = new GetUserBatchOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#editUser(org.swellrt.beta.client.rest.operations.EditUserOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void editUser(EditUserOperation.Options options, Callback<EditUserOperation.Response> callback) {
    EditUserOperation op = new EditUserOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#listLogin(org.swellrt.beta.client.rest.operations.ListLoginOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void listLogin(ListLoginOperation.Options options, Callback<ListLoginOperation.Response> callback) {
    ListLoginOperation op = new ListLoginOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#recoverPassword(org.swellrt.beta.client.rest.operations.PasswordRecoverOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void recoverPassword(PasswordRecoverOperation.Options options, Callback<PasswordRecoverOperation.Response> callback) {
    PasswordRecoverOperation op = new PasswordRecoverOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#password(org.swellrt.beta.client.rest.operations.PasswordOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void password(PasswordOperation.Options options, Callback<PasswordOperation.Response> callback) {
    PasswordOperation op = new PasswordOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#getObjectNames(org.swellrt.beta.client.rest.operations.naming.GetNamesOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void getObjectNames(GetNamesOperation.Options options,
      Callback<GetNamesOperation.Response> callback) {
    GetNamesOperation op = new GetNamesOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#setObjectName(org.swellrt.beta.client.rest.operations.naming.SetNameOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void setObjectName(SetNameOperation.Options options,
      Callback<SetNameOperation.Response> callback) {
    SetNameOperation op = new SetNameOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#deleteObjectName(org.swellrt.beta.client.rest.operations.naming.DeleteNameOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void deleteObjectName(DeleteNameOperation.Options options,
      Callback<DeleteNameOperation.Response> callback) {
    DeleteNameOperation op = new DeleteNameOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#getProfilesManager()
   */
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

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#getAppDomain()
   */
  @Override
  public String getAppDomain() {
    return context.getWaveDomain();
  }
}
