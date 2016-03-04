package org.swellrt.server.box.servlet;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.NotImplementedException;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.ConversionUtils;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.generic.ResourceTool;
import org.swellrt.server.velocity.CustomResourceTool;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class EmailService extends SwellRTService {

  public static final String EMAIL = "email";

  private static final String METHOD = "method";

  private static final String SET = "set";

  private static final String PASSWORD_RESET = "password-reset";

  private static final String RECOVER_URL = "recover-url";

  /*
   * Path that has the default templates and translations inside the classpath
   */
  private static final String CLASSPATH_VELOCITY_PATH = "org/swellrt/server/velocity/";

  private static final String RECOVER_PASSWORD_TEMPLATE = "RecoverPassword.vm";

  private static final Log LOG = Log.get(EmailService.class);

  private static final String RECOVER_PASSWORD_BUNDLE = "PasswordRestoreMessages";


  private final AccountStore accountStore;
  private final String host;
  private final String from;
  private final String recoverPasswordTemplateName;
  private final VelocityEngine ve;
  private URLClassLoader loader;
  private Session mailSession;
  private ToolManager manager;

  private String recoverPasswordMessages;

  @Inject
  public EmailService(SessionManager sessionManager, AccountStore accountStore,
      @Named(CoreSettings.EMAIL_HOST) String host,
      @Named(CoreSettings.EMAIL_FROM_ADDRESS) String from,
      @Named(CoreSettings.VELOCITY_PATH) String velocityPath) {
    super(sessionManager);
    this.accountStore = accountStore;
    this.host = host;
    this.from = from;


    Properties p = new Properties();
    p.put("resource.loader", "file, class");
    p.put("file.resource.loader.class",
        "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
    p.put("file.resource.loader.path", velocityPath);
    p.put("class.resource.loader.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

    ve = new VelocityEngine();

    ve.init(p);

    this.recoverPasswordTemplateName = ve.resourceExists("RecoverPassword.vm")
        ? RECOVER_PASSWORD_TEMPLATE : CLASSPATH_VELOCITY_PATH + RECOVER_PASSWORD_TEMPLATE;

    this.recoverPasswordMessages =
        new File(velocityPath + RECOVER_PASSWORD_BUNDLE + ".properties").exists()
            ? RECOVER_PASSWORD_BUNDLE
            : CLASSPATH_VELOCITY_PATH.replace("/", ".") + RECOVER_PASSWORD_BUNDLE;


    try {
      // based on http://stackoverflow.com/a/15654598/4928558
      File file = new File(velocityPath);
      URL[] urls = {file.toURI().toURL()};
      loader = new URLClassLoader(urls);
    } catch (MalformedURLException e) {
      LOG.warning("Error constructing classLoader for velocity internationalization resources:"
          + e.getMessage());
    }

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
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req.getSession(false));

    if (participantId == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    Enumeration<String> paramNames = req.getParameterNames();

    if (!paramNames.hasMoreElements()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No parameters found!");
      return;
    } else {

      String method = req.getParameter(METHOD);
      String email = req.getParameter(EMAIL);

      switch (method) {

        case SET:
          HttpSession session = req.getSession(false);
          HumanAccountData account = sessionManager.getLoggedInAccount(session).asHuman();

          if (account != null && account.getId().isAnonymous()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User is anonymous");
            return;
          }

          try {

            account.setEmail(email);
            accountStore.putAccount(account);
            response.setStatus(HttpServletResponse.SC_OK);

          } catch (IllegalArgumentException t) {

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, t.getMessage());

          } catch (PersistenceException e) {

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

          }

          break;

        case PASSWORD_RESET:

          String recoverUrl = URLDecoder.decode(req.getParameter(RECOVER_URL), "UTF-8");
          String idOrEmail = URLDecoder.decode(req.getParameter("id-or-email"), "UTF-8");

          String subject = null;

          String htmlBody = null;

          try {

            List<AccountData> accounts = null;
            try {
              accounts = accountStore.getAccountByEmail(idOrEmail);
            } catch (NotImplementedException e) {
              response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            // try to find by username if not found by email
            if (accounts == null || accounts.isEmpty()) {
              AccountData acc = accountStore.getAccount(new ParticipantId(idOrEmail));

              if (acc != null && !acc.getId().isAnonymous()) {
                accounts.add(acc);
              }
            }

            if (accounts != null && !accounts.isEmpty()) {

              for (AccountData a : accounts) {

                String userAddress = a.getId().getAddress();

                double random = Math.random();

                String token =
                    Base64.encodeBase64URLSafeString((String.valueOf(random)).getBytes());

                a.asHuman().setRecoveryToken(token);
                accountStore.putAccount(a);


                if (recoverUrl.contains("$user-id")) {
                  recoverUrl = recoverUrl.replaceAll("\\$user-id", userAddress);
                }

                if (recoverUrl.contains("$token")) {
                  recoverUrl = recoverUrl.replaceAll("\\$token", token);

                } else {
                  recoverUrl = recoverUrl + token;
                }

                MimeMessage message = new MimeMessage(mailSession);

                try {

                  Map<String, Object> ctx = new HashMap<String, Object>();

                  String loc = a.asHuman().getLocale();

                  Locale locale = null;

                  if (loc == null) {
                    locale = Locale.getDefault();
                  } else {
                    locale = ConversionUtils.toLocale(loc);
                  }

                  ctx.put("locale", locale);

                  ctx.put(CustomResourceTool.CLASS_LOADER_KEY, loader);

                  ctx.put(ResourceTool.BUNDLES_KEY, recoverPasswordMessages);

                  Context context = manager.createContext(ctx);

                  context.put("recoverUrl", recoverUrl);
                  context.put("userName", userAddress);


                  Template template = null;

                  try {

                    template = ve.getTemplate(recoverPasswordTemplateName);

                    StringWriter sw = new StringWriter();

                    template.merge(context, sw);

                    sw.flush();

                    htmlBody = sw.toString();

                    ResourceBundle bundle =
                        ResourceBundle.getBundle(recoverPasswordMessages, locale, loader);

                    subject = MessageFormat.format(bundle.getString("emailSubject"), userAddress);

                  } catch (ResourceNotFoundException rnfe) {
                    // couldn't find the template
                    LOG.warning("velocity template not fould");
                    return;
                  }
                } catch (Exception e) {
                  LOG.severe(
                      "Unexpected error while composing email with velocity. The email was not sent. "
                          + e.toString());
                  return;
                }

                message.setFrom(new InternetAddress(from));

                message.addRecipient(Message.RecipientType.TO, new InternetAddress(idOrEmail));

                message.setSubject(subject);
                message.setText(htmlBody, "UTF-8", "html");

                LOG.info(
                    "Sending email:" + "\n  Subject: " + subject + "\n  Message body: " + htmlBody);
                // Send message
                Transport.send(message);

              }
            }

            response.setStatus(HttpServletResponse.SC_OK);

          } catch (MessagingException mex) {

            LOG.severe("Unexpected messaging exception while sending email:" + mex.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, mex.getMessage());

          } catch (PersistenceException e) {

            LOG.severe("Unexpected persistence exception while sending email:" + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

          }

          break;
      }
    }
  }
}
