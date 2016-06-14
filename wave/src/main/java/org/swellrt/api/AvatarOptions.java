package org.swellrt.api;

import com.google.gwt.core.client.JavaScriptObject;

public class AvatarOptions extends JavaScriptObject {


  public static native AvatarOptions create(JavaScriptObject substrate) /*-{
    return substrate;
  }-*/;

  protected AvatarOptions() {
    // Nothing to do
  }

  /**
   * Size in pixels of each avatar's box.
   * 
   * @return
   */
  public final native int getSize() /*-{
    if (this.size)
      return this.size;
    else
      return 40;
  }-*/;

  /**
   * Padding for composite avatars in pixels.
   * 
   * @return
   */
  public final native int getPadding() /*-{
    if (this.padding)
      return this.padding;
    else
      return 1;
  }-*/;

  /**
   * CSS class to add to each avatar element.
   * 
   * @return
   */
  public final native String getCssClass() /*-{
    if (this.cssClass)
      return this.cssClass;
    else
      return null;
  }-*/;

  /**
   * Number of avatars to be generated.
   * 
   * @return
   */
  public final native int getNumberOfAvatars() /*-{
    if (this.numberOfAvatars)
      return this.numberOfAvatars;
    else
      return 1;
  }-*/;

}
