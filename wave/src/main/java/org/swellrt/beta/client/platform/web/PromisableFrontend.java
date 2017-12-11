package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.ServiceConnection;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.platform.web.browser.Promise;
import org.swellrt.beta.client.platform.web.browser.Promise.ConstructorParam;
import org.swellrt.beta.client.platform.web.browser.Promise.FunctionParam;
import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.GetUserBatchOperation;
import org.swellrt.beta.client.rest.operations.ListLoginOperation;
import org.swellrt.beta.client.rest.operations.QueryOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.client.rest.operations.params.CredentialData;
import org.swellrt.beta.client.rest.operations.params.ObjectId;
import org.swellrt.beta.client.rest.operations.params.ObjectName;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SObject;
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
public class PromisableFrontend implements ServiceConnection {

  private final ServiceFrontend service;

  public PromisableFrontend(ServiceFrontend service) {
		this.service = service;
	}

  public Promise<Account, SException> createUser(Account options) {

    return new Promise<Account, SException>(new ConstructorParam<Account, SException>() {

			@Override
      public void exec(FunctionParam<Account> resolve, FunctionParam<SException> reject) {

        service.createUser(options, new Callback<Account>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
          public void onSuccess(Account response) {
						resolve.exec(response);
					}
				});

			}
		});
	}


  public Promise<Account, SException> login(Credential options) {
    return new Promise<Account, SException>(new ConstructorParam<Account, SException>() {

			@Override
      public void exec(FunctionParam<Account> resolve, FunctionParam<SException> reject) {

        service.login(options, new Callback<Account>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
          public void onSuccess(Account response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

  public Promise<Void, SException> logout(Credential options) {
    return new Promise<Void, SException>(new ConstructorParam<Void, SException>() {

			@Override
      public void exec(FunctionParam<Void> resolve, FunctionParam<SException> reject) {

        service.logout(options, new Callback<Void>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
          public void onSuccess(Void response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

  public Promise<Account, SException> resume(Credential options) {
    return new Promise<Account, SException>(new ConstructorParam<Account, SException>() {

			@Override
      public void exec(FunctionParam<Account> resolve, FunctionParam<SException> reject) {

        service.resume(options, new Callback<Account>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
          public void onSuccess(Account response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

  public Promise<SObject, SException> open(ObjectId options) {
    return new Promise<SObject, SException>(new ConstructorParam<SObject, SException>() {

			@Override
          public void exec(FunctionParam<SObject> resolve, FunctionParam<SException> reject) {

            service.open(options, new Callback<SObject>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
              public void onSuccess(SObject response) {
						resolve.exec(response);
					}
				});

			}
		});
	}

  public Promise<Void, SException> close(ObjectId options) {
    return new Promise<Void, SException>(new ConstructorParam<Void, SException>() {

			@Override
      public void exec(FunctionParam<Void> resolve, FunctionParam<SException> reject) {

        service.close(options, new Callback<Void>() {
					@Override
					public void onError(SException exception) {
						reject.exec(exception);
					}
					@Override
          public void onSuccess(Void response) {
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

  public Promise<Account, SException> getUser(Credential options) {
    return new Promise<Account, SException>(new ConstructorParam<Account, SException>() {

      @Override
      public void exec(FunctionParam<Account> resolve, FunctionParam<SException> reject) {

        service.getUser(options, new Callback<Account>() {
          @Override
          public void onError(SException exception) {
            reject.exec(exception);
          }
          @Override
          public void onSuccess(Account response) {
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

  public Promise<Account, SException> editUser(Account options) {
    return new Promise<Account, SException>(new ConstructorParam<Account, SException>() {

	    @Override
      public void exec(FunctionParam<Account> resolve,
              FunctionParam<SException> reject) {

        service.editUser(options, new Callback<Account>() {
	        @Override
	        public void onError(SException exception) {
	          reject.exec(exception);
	        }
	        @Override
          public void onSuccess(Account response) {
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

  public Promise<Void, SException> recoverPassword(CredentialData options) {
    return new Promise<Void, SException>(new ConstructorParam<Void, SException>() {

	      @Override
      public void exec(FunctionParam<Void> resolve, FunctionParam<SException> reject) {

        service.recoverPassword(options, new Callback<Void>() {
	          @Override
	          public void onError(SException exception) {
	            reject.exec(exception);
	          }
	          @Override
          public void onSuccess(Void response) {
	            resolve.exec(response);
	          }
	        });

	      }
	    });
	  }

  public Promise<Void, SException> password(CredentialData options) {
    return new Promise<Void, SException>(new ConstructorParam<Void, SException>() {

       @Override
      public void exec(FunctionParam<Void> resolve, FunctionParam<SException> reject) {

        service.password(options, new Callback<Void>() {
           @Override
           public void onError(SException exception) {
             reject.exec(exception);
           }
           @Override
          public void onSuccess(Void response) {
             resolve.exec(response);
           }
         });

       }
     });
   }

  public Promise<Void, SException> getObjectNames(ObjectName options) {

    return new Promise<Void, SException>(new ConstructorParam<Void, SException>() {

          @Override
      public void exec(FunctionParam<Void> resolve,
              FunctionParam<SException> reject) {

        service.getObjectNames(options, new Callback<Void>() {
              @Override
              public void onError(SException exception) {
                reject.exec(exception);
              }

              @Override
          public void onSuccess(Void response) {
                resolve.exec(response);
              }
            });

          }
        });

  }

  public Promise<Void, SException> setObjectName(ObjectName options) {

    return new Promise<Void, SException>(new ConstructorParam<Void, SException>() {

          @Override
      public void exec(FunctionParam<Void> resolve,
              FunctionParam<SException> reject) {

        service.setObjectName(options, new Callback<Void>() {
              @Override
              public void onError(SException exception) {
                reject.exec(exception);
              }

              @Override
          public void onSuccess(Void response) {
                resolve.exec(response);
              }
            });

          }
        });

  }

  public Promise<Void, SException> deleteObjectName(ObjectName options) {

    return new Promise<Void, SException>(new ConstructorParam<Void, SException>() {

          @Override
      public void exec(FunctionParam<Void> resolve,
              FunctionParam<SException> reject) {

        service.deleteObjectName(options, new Callback<Void>() {
              @Override
              public void onError(SException exception) {
                reject.exec(exception);
              }

              @Override
          public void onSuccess(Void response) {
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

  public String getAppDomain() {
    return service.getAppDomain();
  }
}
