package org.waveprotocol.box.server.account;

import java.util.Date;

/**
 * A secret token with creation date to be used for authenticate an user without
 * password (i.e. for restore her password from her email address)
 *
 * @author antoniotenorio@ucm.es (Antonio Tenorio-Fornés)
 *
 */
public class SecretToken {

  private final String token;
  private final Date expirationDate;

  public SecretToken(String token) {

    this.token = token;

    // 24 hours expiration date
    this.expirationDate = new Date(new Date().getTime() + 24 * 60 * 60 * 1000);

  }

  public String getToken() {
    return this.token;
  }

  public Date getExpirationDate() {
    return this.expirationDate;
  }

}
