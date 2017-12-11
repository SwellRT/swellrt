package org.swellrt.beta.client.rest.operations.params;

public class AccountImpl implements Account {

  protected String id;
  protected String email;
  protected String locale;
  protected String avatarUrl;
  protected String sessionId;
  protected String transientSessionId;
  protected String domain;
  protected String name;
  protected String password;
  protected String avatarData;

  public AccountImpl() {

  }

  public AccountImpl(String id) {
    this.id = id;
  }

  public AccountImpl(String id, String email, String locale, String avatarUrl, String sessionId,
      String transientSessionId, String domain, String name, String password, String avatarData) {
    super();
    this.id = id;
    this.email = email;
    this.locale = locale;
    this.avatarUrl = avatarUrl;
    this.sessionId = sessionId;
    this.transientSessionId = transientSessionId;
    this.domain = domain;
    this.name = name;
    this.password = password;
    this.avatarData = avatarData;
  }

  public AccountImpl(String id, String email, String locale, String avatarUrl,
      String sessionId, String transientSessionId, String domain, String name) {
    super();
    this.id = id;
    this.email = email;
    this.locale = locale;
    this.avatarUrl = avatarUrl;
    this.sessionId = sessionId;
    this.transientSessionId = transientSessionId;
    this.domain = domain;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getLocale() {
    return locale;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getTransientSessionId() {
    return transientSessionId;
  }

  public String getDomain() {
    return domain;
  }

  public String getName() {
    return name;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getAvatarData() {
    return avatarData;
  }

}
