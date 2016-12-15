package org.swellrt.beta.client.js;

import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.js.Promise.ConstructorParam;
import org.swellrt.beta.client.js.Promise.FunctionParam;
import org.swellrt.beta.client.operation.OperationException;
import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.impl.CloseOperation;
import org.swellrt.beta.client.operation.impl.CreateUserOperation;
import org.swellrt.beta.client.operation.impl.LoginOperation;
import org.swellrt.beta.client.operation.impl.LogoutOperation;
import org.swellrt.beta.client.operation.impl.OpenOperation;
import org.swellrt.beta.client.operation.impl.QueryOperation;
import org.swellrt.beta.client.operation.impl.ResumeOperation;

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

	public Promise<CreateUserOperation.Response, OperationException> createUser(CreateUserOperation.Options options) {
		
		return new Promise<CreateUserOperation.Response, OperationException>(new ConstructorParam<CreateUserOperation.Response, OperationException>() {
			
			@Override
			public void exec(FunctionParam<CreateUserOperation.Response> resolve, FunctionParam<OperationException> reject) {

				service.createUser(options, new Callback<CreateUserOperation.Response>() {
					@Override
					public void onError(OperationException exception) {
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
	

	public Promise<LoginOperation.Response, OperationException> login(LoginOperation.Options options) {
		return new Promise<LoginOperation.Response, OperationException>(new ConstructorParam<LoginOperation.Response, OperationException>() {
			
			@Override
			public void exec(FunctionParam<LoginOperation.Response> resolve, FunctionParam<OperationException> reject) {

				service.login(options, new Callback<LoginOperation.Response>() {
					@Override
					public void onError(OperationException exception) {
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

	public Promise<LogoutOperation.Response, OperationException> logout(LogoutOperation.Options options) {
		return new Promise<LogoutOperation.Response, OperationException>(new ConstructorParam<LogoutOperation.Response, OperationException>() {
			
			@Override
			public void exec(FunctionParam<LogoutOperation.Response> resolve, FunctionParam<OperationException> reject) {

				service.logout(options, new Callback<LogoutOperation.Response>() {
					@Override
					public void onError(OperationException exception) {
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

	public Promise<ResumeOperation.Response, OperationException> resume(ResumeOperation.Options options) {
		return new Promise<ResumeOperation.Response, OperationException>(new ConstructorParam<ResumeOperation.Response, OperationException>() {
			
			@Override
			public void exec(FunctionParam<ResumeOperation.Response> resolve, FunctionParam<OperationException> reject) {

				service.resume(options, new Callback<ResumeOperation.Response>() {
					@Override
					public void onError(OperationException exception) {
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

	public Promise<OpenOperation.Response, OperationException> open(OpenOperation.Options options) {
		return new Promise<OpenOperation.Response, OperationException>(new ConstructorParam<OpenOperation.Response, OperationException>() {
			
			@Override
			public void exec(FunctionParam<OpenOperation.Response> resolve, FunctionParam<OperationException> reject) {

				service.open(options, new Callback<OpenOperation.Response>() {
					@Override
					public void onError(OperationException exception) {
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

	public Promise<CloseOperation.Response, OperationException> close(CloseOperation.Options options) {
		return new Promise<CloseOperation.Response, OperationException>(new ConstructorParam<CloseOperation.Response, OperationException>() {
			
			@Override
			public void exec(FunctionParam<CloseOperation.Response> resolve, FunctionParam<OperationException> reject) {

				service.close(options, new Callback<CloseOperation.Response>() {
					@Override
					public void onError(OperationException exception) {
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

	public Promise<QueryOperation.Response, OperationException> query(QueryOperation.Options options) {
		return new Promise<QueryOperation.Response, OperationException>(new ConstructorParam<QueryOperation.Response, OperationException>() {
			
			@Override
			public void exec(FunctionParam<QueryOperation.Response> resolve, FunctionParam<OperationException> reject) {

				service.query(options, new Callback<QueryOperation.Response>() {
					@Override
					public void onError(OperationException exception) {
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

	
}
