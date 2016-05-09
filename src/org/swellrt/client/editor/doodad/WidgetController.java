package org.swellrt.client.editor.doodad;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

/**
 * JavaScript object implementing a generic doodad aka Widget. It must provide
 * following methods:
 * 
 * <pre>
 * onInit()
 * </pre>
 * 
 * The widget is active in the editor. Perform render inside the parent HTML
 * element.
 * 
 * <pre>
 * onChangeState(...)
 * </pre>
 * 
 * The state has changed by a participant. Perform render update inside the
 * parent HTML element.
 * 
 * 
 * The widget's code must handle DOM events setting event listener in specific
 * elements. Widget should stop propagation of events inside the Widget.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public class WidgetController extends JavaScriptObject {


  /**
   * For demo only purposes. An example of (stupid) Widget controller. It shows
   * the state as a text with a strong background color. On click you can change
   * the state.
   *
   * @return
   */
  public final static native WidgetController getDemoWidgetController() /*-{

    var wc = {

      onInit: function(parentElement, state) {
        parentElement.innerHTML="<span style='background: #FFD677;'>"+state+"</span>";
      },

      onChangeState: function(parentElement, before, after) {
        parentElement.innerHTML="<span style='background: #FFD677;'>"+after+"</span>";
      }

    };

    return wc;

  }-*/;



  protected WidgetController() {

  }

  //
  // Basic Widgets
  //

  public final native void onInit(Element parent, String state)  /*-{
    this.onInit(parent, state);
  }-*/;

  public final native void onChangeState(Element parent, String before, String after) /*-{
    this.onChangeState(parent, before, after);
  }-*/;


}
