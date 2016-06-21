package org.swellrt.server.box.servlet;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.typesafe.config.Config;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.generic.ResourceTool;
import org.swellrt.server.velocity.CustomResourceTool;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.wave.util.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

public class EmailSenderImp implements EmailSender, DecoupledTemplates {

  private static final Log LOG = Log.get(EmailSenderImp.class);

  private AccountStore accountStore;

  private String host;

  private String from;

  private Session mailSession;
  private ToolManager manager;

  private VelocityEngine ve;

  private String velocityPath;

  private ClassLoader propertyClassloader;
  private boolean isExternalPropertyClassLoader = false;


  @Inject
  public EmailSenderImp(SessionManager sessionManager, AccountStore accountStore, VelocityEngine ve, Config config) {

    this.accountStore = accountStore;
    this.ve = ve;
    this.velocityPath = config.getString("email.template_path");
    this.host = config.getString("email.host");
    this.from = config.getString("email.from_email_address");

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

    try {
      // based on http://stackoverflow.com/a/15654598/4928558
      File file = new File(velocityPath);
      if (!file.exists())
    	  throw new IOException("Folder for email template files not found!");
      URL[] urls = {file.toURI().toURL()};
      propertyClassloader = new URLClassLoader(urls);
      isExternalPropertyClassLoader = true;
    } catch (IOException e) {
      LOG.warning("Loading default email template files"+e.getMessage());
      propertyClassloader = getClass().getClassLoader();
      isExternalPropertyClassLoader = false;
    }

  }

  @Override
  public void send(InternetAddress address, String subject, String htmlBody)
      throws AddressException, MessagingException {


    MimeMessage message = new MimeMessage(mailSession);

    message.setFrom(new InternetAddress(from));

    message.addRecipient(Message.RecipientType.TO, address);

    message.setSubject(subject);

    message.setText(htmlBody, "UTF-8", "html");

    LOG.info("Sending email:" + "\n  Subject: " + subject + "\n  Message body: " + htmlBody);
    // Send message
    Transport.send(message);
  }

  @Override
  public Template getTemplateFromName(String templateName) {
    String path =
        ve.resourceExists(templateName) ? templateName : CLASSPATH_VELOCITY_PATH + templateName;

    return ve.getTemplate(path);

  }

  @Override
  public ResourceBundle getBundleFromName(String messageBundleName, Locale locale) {

    if (locale == null) {
      locale = Locale.getDefault();
    }
    	
    return ResourceBundle.getBundle(getDecoupledBundleName(messageBundleName), locale, propertyClassloader);

  }

  @Override
  public String getDecoupledBundleName(String messageBundleName) {
	  if (!isExternalPropertyClassLoader)
    	return CLASSPATH_VELOCITY_PATH.replace("/", ".") + messageBundleName;
	   else 
	    return  messageBundleName;
  }

  @Override
  public String getTemplateMessage(Template template, String messageBundleName,
      Map<String, Object> params, Locale locale) {

    Map<String, Object> ctx = new HashMap<String, Object>();

    if (locale == null) {
      locale = Locale.getDefault();
    }
    ctx.put("locale", locale);

    ctx.put(ResourceTool.BUNDLES_KEY, getDecoupledBundleName(messageBundleName));

    ctx.put(CustomResourceTool.CLASS_LOADER_KEY, propertyClassloader);

    Context context = manager.createContext(ctx);

    Iterator<Map.Entry<String, Object>> it = params.entrySet().iterator();

    while (it.hasNext()) {
      Entry<String, Object> p = it.next();
      context.put(p.getKey(), p.getValue());
    }


    StringWriter sw = new StringWriter();

    template.merge(context, sw);

    sw.flush();

    return sw.toString();

  };

}

