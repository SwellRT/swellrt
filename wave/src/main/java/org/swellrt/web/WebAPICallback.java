package org.swellrt.web;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A class to handle responses of async calls to GWT methods
 * of {@WebAPIContext} from Javascript native code of {@WebAPI}
 */

public class WebAPICallback extends JavaScriptObject {

  protected WebAPICallback() {

  }

  public native final void onSuccess(JavaScriptObject o)  /*-{

    if (this.onSuccess && typeof this.onSuccess === "function") {
      this.onSuccess(o);
    }

  }-*/;

  public native final void onFailure(JavaScriptObject o)  /*-{

    if (this.onFailure && typeof this.onFailure === "function") {
      this.onFailure(o);
    }

  }-*/;



}

