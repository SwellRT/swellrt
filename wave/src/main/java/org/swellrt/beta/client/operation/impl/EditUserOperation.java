package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ServerAccountData;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class EditUserOperation extends HTTPOperation<EditUserOperation.Options, EditUserOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options  {

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

  public static final class JsoOptions extends JavaScriptObject implements Options  {

    protected JsoOptions() {

    }

    @Override
    public String getId() {
      return JsoView.as(this).getString("id");
    }

    @Override
    public String getPassword() {
      return JsoView.as(this).getString("password");
    }

    @Override
    public String getEmail() {
      return JsoView.as(this).getString("email");
    }

    @Override
    public String getLocale() {
      return JsoView.as(this).getString("locale");
    }

    @Override
    public String getName() {
      return JsoView.as(this).getString("name");
    }

    @Override
    public String getAvatarData() {
      return JsoView.as(this).getString("avatarData");
    }

  }

  @JsType(isNative = true)
  public interface Response extends Operation.Response, ServerAccountData {

  }


  public EditUserOperation(ServiceContext context) {
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
        options.getId() == null) {

      if (callback != null)
    	  callback.onError(new SException(SException.MISSING_PARAMETERS));
    }

    addPathElement("account");
    addPathElement(options.getId());
    setBody(generateBody(options));
    executePost(callback);
  }



}
