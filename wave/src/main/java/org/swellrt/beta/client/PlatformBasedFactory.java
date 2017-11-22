package org.swellrt.beta.client;

import org.swellrt.beta.client.js.editor.STextWeb;
import org.swellrt.beta.model.SText;

/**
 * A separated place to create platform dependent types of the SwellRT model.
 * <p>
 * The aim of this class is to avoid any platform dependent decision in the rest
 * of the classes of model.* package.
 * <p>
 * Obviously, this class must be adapted for each platform (Web, Android...)
 * <p>
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface PlatformBasedFactory {


  public static void copySTextContent(SText source, SText target) {
    if (target instanceof STextWeb) {
      STextWeb targetWeb = (STextWeb) target;
      targetWeb.getContentDocument().consume(source.getInitContent());
    }
  }

}
