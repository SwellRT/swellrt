package org.swellrt.server.box.servlet;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class EmailModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EmailSender.class).to(EmailSenderImp.class).in(Singleton.class);
  }

}
