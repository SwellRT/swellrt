package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public final class EditUserOperation
    extends ServerOperation<EditUserOperation.Options, EditUserOperation.Response> {


  @JsType(isNative = true)
  public static class Options extends AccountDataResponse implements ServerOperation.Options {

    public String avatarData;
    public String password;

    @JsOverlay
    public final String getPassword() {
      return password;
    }

    @JsOverlay
    public final String getAvatarData() {
      return avatarData;
    }

  }


  @JsType(isNative = true)
  public static class Response extends AccountDataResponse implements ServerOperation.Response {
  }


  public EditUserOperation(ServiceContext context, Options options,
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

    if (!context.isSession()) {
      throw new SException(SException.NOT_LOGGED_IN);
    }

    addPathElement("account");
    addPathElement(context.getParticipantId());
  }



}
