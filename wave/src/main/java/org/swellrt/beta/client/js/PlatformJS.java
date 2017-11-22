package org.swellrt.beta.client.js;

import org.swellrt.beta.common.Platform;

public class PlatformJS extends Platform {

  @Override
  public boolean isWeb() {
    return false;
  }

  @Override
  public boolean isJavaScript() {
    return true;
  }

  @Override
  public boolean isJava() {
    return false;
  }

}
