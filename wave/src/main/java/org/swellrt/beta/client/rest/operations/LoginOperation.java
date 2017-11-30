package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ServerAccountData;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Log in operation
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public final class LoginOperation
    extends ServerOperation<LoginOperation.Options, LoginOperation.Response> {

  public LoginOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }

  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    @JsProperty
    public String getId();

    @JsProperty
    public String getPassword();

    @JsProperty
    public boolean getRemember();

  }

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response, ServerAccountData {

  }

  public void doSuccess(LoginOperation.Response response) {
    getContext().init(response);
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
