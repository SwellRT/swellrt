package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class PasswordOperation
    extends ServerOperation<PasswordOperation.Options, PasswordOperation.Response> {



  public PasswordOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }

  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    @JsProperty
    public String getId();

    @JsProperty
    public String getOldPassword();

    @JsProperty
    public String getNewPassword();

    @JsProperty
    public String getToken();

  }

  @JsType(isNative = true)
  public static final class Response implements ServerOperation.Response {
  }


  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  protected void buildRestParams() throws SException {

    PasswordOperation.Options options = getOptions();

    addPathElement("password");
    addQueryParam("id", options.getId());

    if (options.getOldPassword() != null) {
      addQueryParam("token-or-password", options.getOldPassword());
    } else if (options.getToken() != null) {
      addQueryParam("token-or-password", options.getToken());
    } else {
      throw new SException(SException.MISSING_PARAMETERS, null,
          "Missing token or old password parameter");
    }

    addQueryParam("new-password", options.getNewPassword());

  }



}
