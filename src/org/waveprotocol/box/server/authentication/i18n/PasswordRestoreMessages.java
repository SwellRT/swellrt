package org.waveprotocol.box.server.authentication.i18n;

import com.google.gwt.i18n.client.Messages;

public interface PasswordRestoreMessages extends Messages {

  @DefaultMessage("Restore {0} password")
  String emailSubject(String username);

  @DefaultMessage("Hello {0},\n\nTo restore your password, please visit the following link {1} . \n\nIf you did not request a password recovery, you can ignore this message.")
  String restoreEmailBody(String username, String urlWithToken);

}
