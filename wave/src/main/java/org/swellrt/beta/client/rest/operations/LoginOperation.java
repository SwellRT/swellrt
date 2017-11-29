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

    // Chain Login with the Echo operation to check if session cookie is
    // received by the server. The Echo operation will configure the context
    // properly. No further actions are required.
    //

    // TODO call echo service!!!

    // EchoOperation echo = new EchoOperation(getServiceContext());
    // echo.execute(null, new Callback<EchoOperation.Response>() {
    //
    // @Override
    // public void onError(SException exception) {
    // // Ignore error of echo, login was successful anyway
    // if (callback != null) {
    // callback.onSuccess(response);
    // }
    // }
    //
    // @Override
    // public void
    // onSuccess(org.swellrt.beta.client.operation.impl.EchoOperation.Response
    // echoResponse) {
    // if (callback != null) {
    // callback.onSuccess(response);
    // }
    // }
    //
    // });

  }



  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  public void buildRestParams() throws SException {
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
