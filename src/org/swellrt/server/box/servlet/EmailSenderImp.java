package org.swellrt.server.box.servlet;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.generic.ResourceTool;
import org.swellrt.server.velocity.CustomResourceTool;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.wave.util.logging.Log;

import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSenderImp implements EmailSender {

  /*
   * Path that has the default templates and translations inside the classpath
   */
  private static final String CLASSPATH_VELOCITY_PATH = "org/swellrt/server/velocity/";

  private static final Log LOG = Log.get(EmailSenderImp.class);

  private AccountStore accountStore;

  private String host;

  private String from;

  private static Session mailSession;
  private static ToolManager manager;

  private VelocityEngine ve;

  private String velocityPath;

  private static URLClassLoader loader;

  @Inject
  public EmailSenderImp(SessionManager sessionManager, AccountStore accountStore,
      @Named(CoreSettings.VELOCITY_PATH) String velocityPath, VelocityEngine ve,
      @Named(CoreSettings.EMAIL_HOST) String host,
      @Named(CoreSettings.EMAIL_FROM_ADDRESS) String from) {

    this.accountStore = accountStore;
    this.ve = ve;
    this.velocityPath = velocityPath;
    this.host = host;
    this.from = from;

    Properties p = new Properties();
    p.put("resource.loader", "file, class");
    p.put("file.resource.loader.class",
        "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
    p.put("file.resource.loader.path", velocityPath);
    p.put("class.resource.loader.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");



    ve.init(p);


    Properties properties = new Properties();


    // Get the default Session object.
    mailSession = Session.getDefaultInstance(properties, null);

    // Setup mail server
    properties.setProperty("mail.smtp.host", host);
    properties.setProperty("mail.smtp.from", from);

    manager = new ToolManager(false);

    manager.setVelocityEngine(ve);

    manager.configure("velocity-tools-config.xml");

  }

  @Override
  public void send(String address, String templateFileName, String messageBundleFileName,
      HashMap<String, String> params, Locale locale) throws AddressException, MessagingException {

    String templatePath = ve.resourceExists(templateFileName) ? templateFileName
        : CLASSPATH_VELOCITY_PATH + templateFileName;

    String messagesPath = new File(velocityPath + messageBundleFileName + ".properties").exists()
        ? messageBundleFileName : CLASSPATH_VELOCITY_PATH.replace("/", ".") + messageBundleFileName;


    try {
      // based on http://stackoverflow.com/a/15654598/4928558
      File file = new File(velocityPath);
      URL[] urls = {file.toURI().toURL()};
      loader = new URLClassLoader(urls);
    } catch (MalformedURLException e) {
      LOG.warning("Error constructing classLoader for velocity internationalization resources:"
          + e.getMessage());
    }

    MimeMessage message = new MimeMessage(mailSession);

    String htmlBody;
    String subject;
    try {

      Map<String, Object> ctx = new HashMap<String, Object>();

      if (locale == null) {
        locale = Locale.getDefault();
      }

      ctx.put("locale", locale);

      ctx.put(CustomResourceTool.CLASS_LOADER_KEY, loader);

      ctx.put(ResourceTool.BUNDLES_KEY, messagesPath);

      Context context = manager.createContext(ctx);

      // context.put("recoverUrl", recoverUrl);
      // context.put("userName", userAddress);

      Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();

      while (it.hasNext()) {
        Entry<String, String> p = it.next();
        context.put(p.getKey(), p.getValue());
      }


      Template template = null;

      try {

        template = ve.getTemplate(templatePath);

        StringWriter sw = new StringWriter();

        template.merge(context, sw);

        sw.flush();

        htmlBody = sw.toString();

        ResourceBundle bundle = ResourceBundle.getBundle(messagesPath, locale, loader);

        subject = MessageFormat.format(bundle.getString("emailSubject"), address);

      } catch (ResourceNotFoundException rnfe) {
        // couldn't find the template
        LOG.warning("velocity template not fould");
        return;
      }
    } catch (Exception e) {
      LOG.severe("Unexpected error while composing email with velocity. The email was not sent. "
          + e.toString());
      return;
    }

    message.setFrom(new InternetAddress(from));

    message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));

    message.setSubject(subject);
    message.setText(htmlBody, "UTF-8", "html");

    LOG.info("Sending email:" + "\n  Subject: " + subject + "\n  Message body: " + htmlBody);
    // Send message
    Transport.send(message);
  }

}

