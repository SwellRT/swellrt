package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ServerAccountData;

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

  public static class DefaultOptions implements Options {

    private String id;
    private String password;
    private String email;
    private String locale;
    private String name;
    private String avatarData;

    public DefaultOptions(String id, String name) {
      super();
      this.id = id;
      this.name = name;
    }

    public DefaultOptions(String id, String password, String email, String locale, String name,
        String avatarData) {
      super();
      this.id = id;
      this.password = password;
      this.email = email;
      this.locale = locale;
      this.name = name;
      this.avatarData = avatarData;
    }

    public String getId() {
      return id;
    }

    public String getPassword() {
      return password;
    }

    public String getEmail() {
      return email;
    }

    public String getLocale() {
      return locale;
    }

    public String getName() {
      return name;
    }

    public String getAvatarData() {
      return avatarData;
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
