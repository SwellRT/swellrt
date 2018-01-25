package org.swellrt.beta.client;


import org.swellrt.beta.client.rest.ClientOperationExecutor;
import org.swellrt.beta.client.rest.ServerOperationExecutor;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.CloseOperation;
import org.swellrt.beta.client.rest.operations.CreateUserOperation;
import org.swellrt.beta.client.rest.operations.DeleteNameOperation;
import org.swellrt.beta.client.rest.operations.EchoOperation;
import org.swellrt.beta.client.rest.operations.EditUserOperation;
import org.swellrt.beta.client.rest.operations.GetNamesOperation;
import org.swellrt.beta.client.rest.operations.GetUserBatchOperation;
import org.swellrt.beta.client.rest.operations.GetUserOperation;
import org.swellrt.beta.client.rest.operations.ListLoginOperation;
import org.swellrt.beta.client.rest.operations.LoginOperation;
import org.swellrt.beta.client.rest.operations.LogoutOperation;
import org.swellrt.beta.client.rest.operations.OpenOperation;
import org.swellrt.beta.client.rest.operations.PasswordOperation;
import org.swellrt.beta.client.rest.operations.PasswordRecoverOperation;
import org.swellrt.beta.client.rest.operations.QueryOperation;
import org.swellrt.beta.client.rest.operations.ResumeOperation;
import org.swellrt.beta.client.rest.operations.SetNameOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.AccountImpl;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.client.rest.operations.params.CredentialData;
import org.swellrt.beta.client.rest.operations.params.CredentialImpl;
import org.swellrt.beta.client.rest.operations.params.ObjectId;
import org.swellrt.beta.client.rest.operations.params.ObjectName;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SObject;
import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.AbstractProfileManager;
import org.waveprotocol.wave.client.common.util.RgbColor;
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
      if (context.hasSession()) {
        return context.getServiceSession().getSessionToken();
      }
      return "";
    }

    @Override
    public ParticipantId getCurrentParticipantId() {
      try {
        return context.getServiceSession().getParticipantId();
      } catch (Exception e) {
        return null;
      }
    }

    @Override
    protected void requestProfile(ParticipantId participantId, RequestProfileCallback callback) {

      Credential options = new CredentialImpl(participantId.getAddress());

      GetUserOperation op = new GetUserOperation(context, options,
          new ServiceOperation.Callback<Account>() {

            @Override
            public void onError(SException exception) {
              logger.log("Error loading profile " + exception.getMessage());
            }

            @Override
            public void onSuccess(Account response) {
              Profile profile = new Profile() {

                @Override
                public ParticipantId getParticipantId() {
                  return ParticipantId.ofUnsafe(response.getId());
                }

                @Override
                public void update(Profile profile) {
                }

                @Override
                public String getAddress() {
                  return response.getId();
                }

                @Override
                public String getName() {
                  return response.getName();
                }

                @Override
                public String getShortName() {
                  return getParticipantId().getName();
                }

                @Override
                public String getImageUrl() {
                  return response.getAvatarUrl();
                }

                @Override
                public void setName(String name) {
                }

                @Override
                public boolean isCurrentSessionProfile() {
                  return false;
                }

                @Override
                public boolean getAnonymous() {
                  return ParticipantId.isAnonymousName(response.getId());
                }

                @Override
                public RgbColor getColor() {
                  return null;
                }

                @Override
                public void trackActivity(String sessionId, double timestamp) {
                }

                @Override
                public void trackActivity(String sessionId) {
                }

                @Override
                public String getEmail() {
                  return response.getEmail();
                }

                @Override
                public String getLocale() {
                  return response.getLocale();
                }

              };
              callback.onCompleted(profile);
            }

          });

      serverOpExecutor.execute(op);

    }

    @Override
    protected void storeProfile(Profile profile) {

      Account options = new AccountImpl();

      EditUserOperation op = new EditUserOperation(context, options,
          new Callback<Account>() {

            @Override
            public void onError(SException exception) {
              logger.log("Error storing profile " + exception.getMessage());
            }

            @Override
            public void onSuccess(Account response) {
            }

          });

      serverOpExecutor.execute(op);
    }

  };

  @JsIgnore
  public DefaultFrontend(ServiceContext context,
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
  public void createUser(Account options, Callback<Account> callback) {
    CreateUserOperation op = new CreateUserOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }


  @Override
  public void login(Credential options, Callback<Account> callback) {

    // Execute an echo after a successful login.

    EchoOperation echoOp = new EchoOperation(context, new Void() {
    }, new Callback<EchoOperation.Response>() {

      @Override
      public void onError(SException exception) {
        logger.log("Echo operation failed: " + exception.getMessage());
      }

      @Override
      public void onSuccess(
          EchoOperation.Response response) {
      }

    });

    LoginOperation loginOp = new LoginOperation(context, options,
        new Callback<Account>() {

          @Override
          public void onError(SException exception) {
            callback.onError(exception);
          }

          @Override
          public void onSuccess(Account response) {
            serverOpExecutor.execute(echoOp);
            callback.onSuccess(response);
          }

        });

    serverOpExecutor.execute(loginOp);

  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#logout(org.swellrt.beta.client.rest.operations.LogoutOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void logout(Credential options, @JsOptional Callback<Void> callback) {
    LogoutOperation op = new LogoutOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#resume(org.swellrt.beta.client.rest.operations.ResumeOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void resume(Credential options, @JsOptional Callback<Account> callback) {
    ResumeOperation op = new ResumeOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#open(org.swellrt.beta.client.rest.operations.OpenOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void open(ObjectId options, Callback<SObject> callback) {
    OpenOperation op = new OpenOperation(context, options, callback);
    clientOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#close(org.swellrt.beta.client.rest.operations.CloseOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void close(ObjectId options, @JsOptional Callback<Void> callback) {
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
  public void getUser(Credential options, Callback<Account> callback) {
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
  public void editUser(Account options,
      Callback<Account> callback) {
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
  public void recoverPassword(CredentialData options, Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    PasswordRecoverOperation op = new PasswordRecoverOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#password(org.swellrt.beta.client.rest.operations.PasswordOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void password(CredentialData options, Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    PasswordOperation op = new PasswordOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#getObjectNames(org.swellrt.beta.client.rest.operations.naming.GetNamesOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void getObjectNames(ObjectName options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    GetNamesOperation op = new GetNamesOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#setObjectName(org.swellrt.beta.client.rest.operations.naming.SetNameOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void setObjectName(ObjectName options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    SetNameOperation op = new SetNameOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#deleteObjectName(org.swellrt.beta.client.rest.operations.naming.DeleteNameOperation.Options, org.swellrt.beta.client.rest.ServiceOperation.Callback)
   */
  @Override
  public void deleteObjectName(ObjectName options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    DeleteNameOperation op = new DeleteNameOperation(context, options, callback);
    serverOpExecutor.execute(op);
  }

  /* (non-Javadoc)
   * @see org.swellrt.beta.client.ServiceFrontend#getProfilesManager()
   */
  @JsProperty
  @Override
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
    return context.getServiceSession().getWaveDomain();
  }
}
