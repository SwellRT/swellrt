package org.swellrt.beta.client;

import java.net.URLDecoder;

import org.swellrt.beta.client.js.PromisableServiceFrontend;
import org.swellrt.beta.client.js.SessionManagerJs;

import com.gargoylesoftware.htmlunit.javascript.host.URL;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

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
  
  
  private static String getServerURL() {
      String url = GWT.getModuleBaseURL();
      
      int c = 3;
      String s = url;
      int index = -1;
      while (c > 0) {
        index = s.indexOf("/", index+1);
        if (index == -1)
          break;
        c--;        
      }
      
      if (c == 0)
        url = url.substring(0, index);
      
      return url;
  }
  
  @Override
  public void onModuleLoad() {    
    this.context = new ServiceContext(SessionManagerJs.create(), getServerURL());  	
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

      @Override
      public void onUncaughtException(Throwable e) {
        // TODO Auto-generated method stub

      }
    });
  }

}
