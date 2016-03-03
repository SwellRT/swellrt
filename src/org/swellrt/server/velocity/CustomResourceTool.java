package org.swellrt.server.velocity;

import org.apache.velocity.tools.ConversionUtils;
import org.apache.velocity.tools.generic.ResourceTool;
import org.apache.velocity.tools.generic.ValueParser;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Locale;
import java.util.ResourceBundle;

public class CustomResourceTool extends ResourceTool {

  public static final String CLASS_LOADER_KEY = "classLoader";

  private static final Log LOG = Log.get(CustomResourceTool.class);

  private ClassLoader classLoader;

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  protected void configure(ValueParser parser) {
    try {
      ClassLoader classLoader = (ClassLoader) parser.getValue(CLASS_LOADER_KEY);

      if (classLoader != null) {
        this.classLoader = classLoader;
      }

    } catch (ClassCastException e) {
    }

    super.configure(parser);
  }

  @Override
  protected ResourceBundle getBundle(String baseName, Object loc) {
    Locale locale = (loc == null) ? getLocale() : toLocale(loc);
    if (baseName == null || locale == null) {
      return null;
    }
    return (classLoader != null) ? ResourceBundle.getBundle(baseName, locale, classLoader)
        : ResourceBundle.getBundle(baseName, locale);

  }



  // Copied from ResourceTool class
  private Locale toLocale(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Locale)
    {
      return (Locale) obj;
    }
    String s = String.valueOf(obj);
    return ConversionUtils.toLocale(s);
  }
}
