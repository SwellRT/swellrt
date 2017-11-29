package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class EchoOperation
    extends ServerOperation<EchoOperation.Options, EchoOperation.Response> {


  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

  }

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response {

    @JsProperty(name = "sessionCookie")
    public boolean isSessionCookie();

  }



  public EchoOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }


  @Override
  public void doSuccess(EchoOperation.Response response) {
    getContext().setSessionCookieAvailability(response.isSessionCookie());
    super.doSuccess(response);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }

  @Override
  public boolean sendSessionInUrl() {
    return false;
  }

  @Override
  public void buildRestParams() throws SException {
    addPathElement("echo");
  }

}
