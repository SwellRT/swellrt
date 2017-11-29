package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import com.google.gwt.safehtml.shared.UriUtils;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class PasswordRecoverOperation
    extends ServerOperation<PasswordRecoverOperation.Options, PasswordRecoverOperation.Response> {



  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    @JsProperty
    public String getId();

    @JsProperty
    public String getEmail();

    @JsProperty
    public String getUrl();

  }

  @JsType(isNative = true)
  public static final class Response implements ServerOperation.Response {

  }

  public PasswordRecoverOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }


  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  public void buildRestParams() throws SException {

    PasswordRecoverOperation.Options options = getOptions();

    addPathElement("email");

    if (options.getId() != null)
      addQueryParam("id-or-email", options.getId());
    else if (options.getEmail() != null)
      addQueryParam("id-or-email", UriUtils.encode(options.getEmail()));
    else
      throw new SException(SException.MISSING_PARAMETERS, null, "User id or email is required");

    addQueryParam("recover-url", options.getUrl());
    addQueryParam("method", "password-reset");

  }

}
