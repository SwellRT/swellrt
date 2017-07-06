package org.swellrt.beta.client.js;

import org.swellrt.beta.client.ServiceBasis;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.js.Promise.ConstructorParam;
import org.swellrt.beta.client.js.Promise.FunctionParam;
import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.impl.CloseOperation;
import org.swellrt.beta.client.operation.impl.CreateUserOperation;
import org.swellrt.beta.client.operation.impl.EditUserOperation;
import org.swellrt.beta.client.operation.impl.GetUserBatchOperation;
import org.swellrt.beta.client.operation.impl.GetUserOperation;
import org.swellrt.beta.client.operation.impl.ListLoginOperation;
import org.swellrt.beta.client.operation.impl.LoginOperation;
import org.swellrt.beta.client.operation.impl.LogoutOperation;
import org.swellrt.beta.client.operation.impl.OpenOperation;
import org.swellrt.beta.client.operation.impl.PasswordOperation;
import org.swellrt.beta.client.operation.impl.PasswordRecoverOperation;
import org.swellrt.beta.client.operation.impl.QueryOperation;
import org.swellrt.beta.client.operation.impl.ResumeOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ProfileManager;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A service fronted returning promises
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swell", name = "PromisableService")
public class PromisableServiceFrontend implements ServiceBasis {

	private final ServiceFrontend service;

	public PromisableServiceFrontend(ServiceFrontend service) {
		this.service = service;
	}

	public Promise<CreateUserOperation.Response, SException> createUser(CreateUserOperation.Options options) {

		return new Promise<CreateUserOperation.Response, SException>(new ConstructorParam<CreateUserOperation.Response, SException>() {

			@Override
			public void exec(FunctionParam<CreateUserOperation.Response> resolve, FunctionParam<SException> reject) {

				service.createUser(options, new Callback<CreateUserOperation.Response>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
					public void onSuccess(CreateUserOperation.Response response) {
						resolve.exec(response);
					}
				});

			}
		});
	}


	public Promise<LoginOperation.Response, SException> login(LoginOperation.Options options) {
		return new Promise<LoginOperation.Response, SException>(new ConstructorParam<LoginOperation.Response, SException>() {

			@Override
			public void exec(FunctionParam<LoginOperation.Response> resolve, FunctionParam<SException> reject) {

				service.login(options, new Callback<LoginOperation.Response>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
					public void onSuccess(LoginOperation.Response response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

	public Promise<LogoutOperation.Response, SException> logout(LogoutOperation.Options options) {
		return new Promise<LogoutOperation.Response, SException>(new ConstructorParam<LogoutOperation.Response, SException>() {

			@Override
			public void exec(FunctionParam<LogoutOperation.Response> resolve, FunctionParam<SException> reject) {

				service.logout(options, new Callback<LogoutOperation.Response>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
					public void onSuccess(LogoutOperation.Response response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

	public Promise<ResumeOperation.Response, SException> resume(ResumeOperation.Options options) {
		return new Promise<ResumeOperation.Response, SException>(new ConstructorParam<ResumeOperation.Response, SException>() {

			@Override
			public void exec(FunctionParam<ResumeOperation.Response> resolve, FunctionParam<SException> reject) {

				service.resume(options, new Callback<ResumeOperation.Response>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
					public void onSuccess(ResumeOperation.Response response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

	public Promise<OpenOperation.Response, SException> open(OpenOperation.Options options) {
		return new Promise<OpenOperation.Response, SException>(new ConstructorParam<OpenOperation.Response, SException>() {

			@Override
			public void exec(FunctionParam<OpenOperation.Response> resolve, FunctionParam<SException> reject) {

				service.open(options, new Callback<OpenOperation.Response>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
					public void onSuccess(OpenOperation.Response response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

	public Promise<CloseOperation.Response, SException> close(CloseOperation.Options options) {
		return new Promise<CloseOperation.Response, SException>(new ConstructorParam<CloseOperation.Response, SException>() {

			@Override
			public void exec(FunctionParam<CloseOperation.Response> resolve, FunctionParam<SException> reject) {

				service.close(options, new Callback<CloseOperation.Response>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
					public void onSuccess(CloseOperation.Response response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

	public Promise<QueryOperation.Response, SException> query(QueryOperation.Options options) {
		return new Promise<QueryOperation.Response, SException>(new ConstructorParam<QueryOperation.Response, SException>() {

			@Override
			public void exec(FunctionParam<QueryOperation.Response> resolve, FunctionParam<SException> reject) {

				service.query(options, new Callback<QueryOperation.Response>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
					public void onSuccess(QueryOperation.Response response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

 public Promise<GetUserOperation.Response, SException> getUser(GetUserOperation.Options options) {
    return new Promise<GetUserOperation.Response, SException>(new ConstructorParam<GetUserOperation.Response, SException>() {

      @Override
      public void exec(FunctionParam<GetUserOperation.Response> resolve, FunctionParam<SException> reject) {

        service.getUser(options, new Callback<GetUserOperation.Response>() {
          @Override
          public void onError(SException exception) {
            reject.exec(exception);
          }
          @Override
          public void onSuccess(GetUserOperation.Response response) {
            resolve.exec(response);
          }
        });

      }
    });
  }

 public Promise<GetUserBatchOperation.Response, SException> getUserBatch(GetUserBatchOperation.Options options) {
   return new Promise<GetUserBatchOperation.Response, SException>(new ConstructorParam<GetUserBatchOperation.Response, SException>() {

     @Override
     public void exec(FunctionParam<GetUserBatchOperation.Response> resolve, FunctionParam<SException> reject) {

       service.getUserBatch(options, new Callback<GetUserBatchOperation.Response>() {
         @Override
         public void onError(SException exception) {
           reject.exec(exception);
         }
         @Override
         public void onSuccess(GetUserBatchOperation.Response response) {
           resolve.exec(response);
         }
       });

     }
   });
 }

	public Promise<EditUserOperation.Response, SException> editUser(EditUserOperation.Options options) {
	  return new Promise<EditUserOperation.Response, SException>(new ConstructorParam<EditUserOperation.Response, SException>() {

	    @Override
	    public void exec(FunctionParam<EditUserOperation.Response> resolve, FunctionParam<SException> reject) {

	      service.editUser(options, new Callback<EditUserOperation.Response>() {
	        @Override
	        public void onError(SException exception) {
	          reject.exec(exception);
	        }
	        @Override
	        public void onSuccess(EditUserOperation.Response response) {
	          resolve.exec(response);
	        }
	      });

	    }
	  });
	}

	public Promise<ListLoginOperation.Response, SException> listLogin(ListLoginOperation.Options options) {
	  return new Promise<ListLoginOperation.Response, SException>(new ConstructorParam<ListLoginOperation.Response, SException>() {

	    @Override
	    public void exec(FunctionParam<ListLoginOperation.Response> resolve, FunctionParam<SException> reject) {

	      service.listLogin(options, new Callback<ListLoginOperation.Response>() {
	        @Override
	        public void onError(SException exception) {
	          reject.exec(exception);
	        }
	        @Override
	        public void onSuccess(ListLoginOperation.Response response) {
	          resolve.exec(response);
	        }
	      });

	    }
	  });
	}

  public Promise<PasswordRecoverOperation.Response, SException> recoverPassword(
      PasswordRecoverOperation.Options options) {
	    return new Promise<PasswordRecoverOperation.Response, SException>(new ConstructorParam<PasswordRecoverOperation.Response, SException>() {

	      @Override
	      public void exec(FunctionParam<PasswordRecoverOperation.Response> resolve, FunctionParam<SException> reject) {

            service.recoverPassword(options, new Callback<PasswordRecoverOperation.Response>() {
	          @Override
	          public void onError(SException exception) {
	            reject.exec(exception);
	          }
	          @Override
              public void onSuccess(PasswordRecoverOperation.Response response) {
	            resolve.exec(response);
	          }
	        });

	      }
	    });
	  }

  public Promise<PasswordOperation.Response, SException> password(
      PasswordOperation.Options options) {
     return new Promise<PasswordOperation.Response, SException>(new ConstructorParam<PasswordOperation.Response, SException>() {

       @Override
       public void exec(FunctionParam<PasswordOperation.Response> resolve, FunctionParam<SException> reject) {

            service.password(options, new Callback<PasswordOperation.Response>() {
           @Override
           public void onError(SException exception) {
             reject.exec(exception);
           }
           @Override
             public void onSuccess(PasswordOperation.Response response) {
             resolve.exec(response);
           }
         });

       }
     });
   }

	@Override
  public void addConnectionHandler(ConnectionHandler h) {
    service.addConnectionHandler(h);
  }

	@Override
  public void removeConnectionHandler(ConnectionHandler h) {
    service.removeConnectionHandler(h);
  }


  @JsProperty
  @Override
  public ProfileManager getProfilesManager() {
    return service.getProfilesManager();
  }

}
