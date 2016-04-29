package org.swellrt.server.box.servlet;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public interface EmailSender {

  void send(InternetAddress address, String subject, String htmlBody)
      throws AddressException, MessagingException;

}
