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

  /**
   * Name of the avatar. First letter (as capital) will be rendered.
   * 
   * @return
   */
  public final native String getName() /*-{
    return this.name;
  }-*/;

  /**
   * URL to a picture to use as avatar.
   * 
   * @return
   */
  public final native String getPicture() /*-{
    return this.picture;
  }-*/;

  /**
   * HTML DOM element to use as avatar.
   * 
   * @return
   */
  public final native Element getElement() /*-{
    return this.element;
  }-*/;


}
