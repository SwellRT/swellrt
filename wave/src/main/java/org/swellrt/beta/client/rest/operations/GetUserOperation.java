package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public final class GetUserOperation
    extends ServerOperation<GetUserOperation.Options, GetUserOperation.Response> {


  @JsType(isNative = true)
  public static class Options implements ServiceOperation.Options {

    public String id;

    @JsOverlay
    public final String getId() {
      return id;
    }

  }

  @JsType(isNative = true)
  public static class Response extends AccountDataResponse implements ServerOperation.Response {
  }

  public GetUserOperation(ServiceContext context, GetUserOperation.Options options,
      ServiceOperation.Callback<GetUserOperation.Response> callback) {
    super(context, options, callback);
  }



  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }


  @Override
  protected void buildRestParams() throws SException {

    if (!context.isSession()) {
      throw new SException(SException.NOT_LOGGED_IN);
    }

    addPathElement("account");
    addPathElement(context.getParticipantId());
  }



}
