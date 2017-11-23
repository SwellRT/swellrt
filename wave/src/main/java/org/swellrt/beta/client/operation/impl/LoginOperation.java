package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.common.Operation;
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
public final class LoginOperation extends HTTPOperation<LoginOperation.Options, LoginOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();

    @JsProperty
    public String getPassword();

    @JsProperty
    public boolean getRemember();

  }

  @JsType(isNative = true)
  public interface Response extends Operation.Response, ServerAccountData {

  }

  public LoginOperation(ServiceContext context) {
    super(context);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    setSessionInURLFlag(false);
    addPathElement("auth");

    setBody(generateBody(options));

    executePost(callback);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null) {
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
    }
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {

    final Response response = generateResponse(data);
    getServiceContext().init(response);

    // Chain Login with the Echo operation to check if session cookie is
    // received by the server. The Echo operation will configure the context
    // properly. No further actions are required.
    //
    EchoOperation echo = new EchoOperation(getServiceContext());
    echo.execute(null, new Callback<EchoOperation.Response>() {

      @Override
      public void onError(SException exception) {
        // Ignore error of echo, login was successful anyway
        if (callback != null) {
          callback.onSuccess(response);
        }
      }

      @Override
      public void onSuccess(org.swellrt.beta.client.operation.impl.EchoOperation.Response echoResponse) {
        if (callback != null) {
          callback.onSuccess(response);
        }
      }

    });


  }


}
