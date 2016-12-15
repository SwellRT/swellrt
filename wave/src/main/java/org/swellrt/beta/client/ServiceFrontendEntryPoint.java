package org.swellrt.beta.client;

import org.swellrt.beta.client.js.PromisableServiceFrontend;
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
  
  private static ServiceContext context; 
  
  @JsMethod(name = "getWithCallback")
  public static ServiceFrontend getStandardInstance() {
    return ServiceFrontend.create(context);
  }
  
  @JsMethod(name = "get")
  public static PromisableServiceFrontend getPromisableInstance() {
    return new PromisableServiceFrontend(getStandardInstance());
  }
  

  @Override
  public void onModuleLoad() {
	this.context = new ServiceContext(SessionManagerJs.create());  
  }
  
}
