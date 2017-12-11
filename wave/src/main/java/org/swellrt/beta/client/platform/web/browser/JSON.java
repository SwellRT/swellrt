package org.swellrt.beta.client.platform.web.browser;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "JSON")
public class JSON {

  native static public <T> T parse(String json);

}
