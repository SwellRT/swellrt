package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class EchoOperation
    extends ServerOperation<Void, EchoOperation.Response> {

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response {

    @JsProperty(name = "sessionCookie")
    public boolean isSessionCookie();

  }

  public static class ResponseImpl implements Response {

    protected boolean sessionCookie;

    public ResponseImpl(boolean sessionCookie) {
      super();
      this.sessionCookie = sessionCookie;
    }

    @Override
    public boolean isSessionCookie() {
      return sessionCookie;
    }

  }


  public EchoOperation(ServiceContext context, Void options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback, ResponseImpl.class);
  }


  @Override
  public void doSuccess(Response response) {
    context.getServiceSession().setSessionCookie(response.isSessionCookie());
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
  protected void buildRestParams() throws SException {
    addPathElement("echo");
    addQueryParam("tid", context.getServiceSession().getTransientSessionId());
  }

}
