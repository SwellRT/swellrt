package org.swellrt.server.box.servlet;

import org.apache.velocity.Template;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * DecoupledTemplates interface allows to operate with Velocity Templates and
 * ResourceBunldes located in the configurable path defined at
 * CoreSettings.VELOCITY_PATH or at the default path CLASSPATH_VELOCITY_PATH
 *
 * @author antonio
 *
 */
public interface DecoupledTemplates {

  /*
   * Path that has the default templates and translations inside the classpath
   */
  public static final String CLASSPATH_VELOCITY_PATH = "org/swellrt/server/velocity/";

  ResourceBundle getBundleFromName(String messageBundleName, Locale locale);

  Template getTemplateFromName(String templateName);

  String getTemplateMessage(Template template, String messageBundleName, Map<String, Object> params,
      Locale locale);

  /*
   * Return the decoupled messageBundle qualified name
   */
  String getDecoupledBundleName(String messageBundleName);

}
