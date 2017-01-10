package org.swellrt.beta.client.js;

import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.ServiceFrontend.ConnectionHandler;
import org.swellrt.beta.client.js.Promise.ConstructorParam;
import org.swellrt.beta.client.js.Promise.FunctionParam;
import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.impl.CloseOperation;
import org.swellrt.beta.client.operation.impl.CreateUserOperation;
import org.swellrt.beta.client.operation.impl.LoginOperation;
import org.swellrt.beta.client.operation.impl.LogoutOperation;
import org.swellrt.beta.client.operation.impl.OpenOperation;
import org.swellrt.beta.client.operation.impl.QueryOperation;
import org.swellrt.beta.client.operation.impl.ResumeOperation;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SHandler;

import jsinterop.annotations.JsType;

/**
 * A service fronted returning promises
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swellrt")
public class PromisableServiceFrontend {

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

  public void setConnectionHandler(ConnectionHandler h) {
    service.setConnectionHandler(h);
  }
  
  public void listen(Object object, SHandler handler) {
    service.listen(object, handler);
  }
  
}
