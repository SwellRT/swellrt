package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.platform.web.browser.JSON;
import org.swellrt.beta.model.json.SJsonObject;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

public class SJsonFactoryWeb implements SJsonObject.Factory {

  @Override
  public SJsonObject create() {
    return new SJsonObjectWeb();
  }

  @Override
  public SJsonObject parse(String json) {
    JsoView jso = JSON.<JsoView> parse(json);
    return new SJsonObjectWeb(jso);
  }

  @Override
  public String serialize(SJsonObject object) {

    if (object instanceof SJsonObjectWeb) {
      SJsonObjectWeb jow = (SJsonObjectWeb) object;
      return JsonUtils.stringify(jow.getJsoView());
    }

    return null;
  }

  @Override
  public SJsonObject create(Object jso) {
    if (jso instanceof JavaScriptObject) {
      return new SJsonObjectWeb(JsoView.as((JavaScriptObject) jso));
    }
    return null;
  }

}
