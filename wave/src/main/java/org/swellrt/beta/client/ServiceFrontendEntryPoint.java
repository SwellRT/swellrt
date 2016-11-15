package org.swellrt.beta.client;

import org.swellrt.beta.client.js.SessionManagerJs;

import com.google.gwt.core.client.EntryPoint;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * A GWT entry point for the service front end.
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swellrt", name = "service")
public class ServiceFrontendEntryPoint extends ServiceFrontend implements EntryPoint {

  private static ServiceFrontend instance = null;
  
  @JsMethod(name = "get")
  public static ServiceFrontend getInstance() {
    return instance;
  }
  

  @Override
  public void onModuleLoad() {
    instance = this;
    instance.setContext(new ServiceContext(SessionManagerJs.create()));
  }
  
}
