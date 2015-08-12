package org.swellrt.api;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;


public class AvatarParameter extends JavaScriptObject {


  public native static AvatarParameter create(JavaScriptObject substrate) /*-{
    return substrate;
  }-*/;

  protected AvatarParameter() {
    // Nothing to do
  }

  public final native String getName() /*-{
    return this.name;
  }-*/;


  public final native String getPictureUrl() /*-{
    return this.pictureUrl;
  }-*/;


  public final native Element getAdditionalElement() /*-{
    return this.additionalElement;
  }-*/;


}
