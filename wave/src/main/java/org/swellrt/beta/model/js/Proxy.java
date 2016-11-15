package org.swellrt.beta.model.js;

import org.swellrt.beta.model.SNode;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class Proxy {

  @JsConstructor
  public Proxy(SNode target, ProxyHandler handler) {    
  }

}
