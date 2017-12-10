package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsType;

/**
 * Log in operation
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType()
public final class LoginOperation
    extends ServerOperation<LoginOperation.Options, LoginOperation.Response> {

  public LoginOperation(ServiceContext context, Options options,
      Callback<Response> callback) {
    super(context, options, callback);
  }

  @JsType(isNative = true)
  public static class Options implements ServerOperation.Options {

    public String id;
    public String password;
    public boolean remember;

  }

  @JsType(isNative = true)
  public static class Response implements ServerOperation.Response {

    public String id;
    public String email;
    public String locale;
    public String avatarUrl;
    public String sessionId;
    public String transientSessionId;
    public String domain;
    public String name;

  }

  @Override
  protected void doSuccess(Response response) {

    /*
     * AccountDataResponse account = new AccountDataResponse();
     *
     * account.avatarUrl = response.avatarUrl; account.domain = response.domain;
     * account.email = response.email; account.id = response.id; account.locale
     * = response.locale; account.name = response.name; account.sessionId =
     * response.sessionId; account.transientSessionId =
     * response.transientSessionId;
     */

    // context.init(null);
    super.doSuccess(response);
  }



  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  protected void buildRestParams() throws SException {
    addPathElement("auth");
  }

  @Override
  public boolean sendSessionInUrl() {
    return false;
  }

  @Override
  public boolean sendOptionsAsBody() {
    return true;
  }

}
