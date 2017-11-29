package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ServerAccountData;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class EditUserOperation
    extends ServerOperation<EditUserOperation.Options, EditUserOperation.Response> {


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

  // TODO avoid JavaScriptObject type here!!! use JsNative = true
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
  public interface Response extends ServerOperation.Response, ServerAccountData {

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
  public void buildRestParams() throws SException {

    if (!getContext().isSession()) {
      doFailure(new SException(SException.NOT_LOGGED_IN));
      return;
    }

    addPathElement("account");
    addPathElement(getContext().getParticipantId());
  }



}
