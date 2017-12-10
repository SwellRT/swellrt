package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsType;

public final class CreateUserOperation
    extends ServerOperation<CreateUserOperation.Options, CreateUserOperation.Response> {


  @JsType(isNative = true)
  public static class Options extends AccountDataResponse implements ServerOperation.Options {

    public String password;

  }

  @JsType(isNative = true)
  public static class Response extends AccountDataResponse {

  }

  public CreateUserOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }


  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  public boolean sendOptionsAsBody() {
    return true;
  }

  @Override
  protected void buildRestParams() throws SException {

    if (options == null || options.id == null || options.password == null) {
      throw new SException(SException.MISSING_PARAMETERS);
    }


    addPathElement("account");
  }



}
