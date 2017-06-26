package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.common.SwellUtils;
import org.waveprotocol.wave.client.account.ServerAccountData;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class CreateUserOperation extends HTTPOperation<CreateUserOperation.Options, CreateUserOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

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
  public interface Response extends Operation.Response, ServerAccountData {

  }


  public CreateUserOperation(ServiceContext context) {
    super(context);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null)
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {
    Response response = generateResponse(data);
    if (callback != null)
      callback.onSuccess(response);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    if (options == null ||
        options.getId() == null ||
        options.getPassword() == null) {

      if (callback != null)
    	  callback.onError(new SException(SException.MISSING_PARAMETERS));
    }

    Options adaptedOptions = new Options() {

      @Override
      public String getId() {
        return SwellUtils.addDomainToParticipant(options.getId(),
            getServiceContext().getWaveDomain());
      }

      @Override
      public String getPassword() {
        return options.getPassword();
      }

      @Override
      public String getEmail() {
        return options.getEmail();
      }

      @Override
      public String getLocale() {
        return options.getLocale();
      }

      @Override
      public String getName() {
        return options.getName();
      }

      @Override
      public String getAvatarData() {
        return options.getAvatarData();
      }

    };

    addPathElement("account");
    setBody(generateBody(adaptedOptions));
    executePost(callback);
  }



}
