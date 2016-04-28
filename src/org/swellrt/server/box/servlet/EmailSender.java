package org.swellrt.server.box.servlet;

import java.util.HashMap;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

public interface EmailSender {

  void send(String address, String templateFileName, String messageBundleFileName,
      HashMap<String, String> params, Locale locale) throws AddressException, MessagingException;

}
