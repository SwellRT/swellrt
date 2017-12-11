package org.swellrt.beta.client.rest.operations.params;

public class CredentialDataImpl implements CredentialData {

  protected String id;
  protected String oldPassword;
  protected String newPassword;
  protected String token;

  protected String email;
  protected String url;

  public CredentialDataImpl() {

  }

  public CredentialDataImpl(String id, String email, String url) {
    super();
    this.id = id;
    this.email = email;
    this.url = url;
  }

  public CredentialDataImpl(String id, String oldPassword, String newPassword, String token) {
    super();
    this.id = id;
    this.oldPassword = oldPassword;
    this.newPassword = newPassword;
    this.token = token;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getOldPassword() {
    return oldPassword;
  }

  @Override
  public String getNewPassword() {
    return newPassword;
  }

  @Override
  public String getToken() {
    return token;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public String getUrl() {
    return url;
  }


}
