package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ServerAccountData;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class CreateUserOperation
    extends ServerOperation<CreateUserOperation.Options, CreateUserOperation.Response> {


  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    @JsProperty
    public String getId();

    @JsProperty
    public String getPassword();

    @JsProperty
    public String getEmail();

    @JsProperty
    public String getLocale();

    @JsProperty
    public String getName();

    @JsProperty
    public String getAvatarData();


  }

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response, ServerAccountData {

  }

  public CreateUserOperation(ServiceContext context, Options options,
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

    if (getOptions() == null || getOptions().getId() == null
        || getOptions().getPassword() == null) {

      throw new SException(SException.MISSING_PARAMETERS);
    }


    addPathElement("account");
  }



}
