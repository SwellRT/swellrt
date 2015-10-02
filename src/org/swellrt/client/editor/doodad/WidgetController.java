package org.swellrt.client.editor.doodad;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

import org.swellrt.client.editor.doodad.WidgetDoodad.WidgetContext;

/**
 * JavaScript object to controll a Widget doodad. It must provide following
 * methods:
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
 * <pre>
 * events.<event_name>(...)
 * </pre>
 *
 * Listener for an event in the Widget UI. 'event_name' must not include the
 * 'on' prefix. A callback function is provided to update the state.
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
      },

      events: {

        click: function(event, context) {
          var newState = prompt("Please enter new value", context.getState());
          context.setState(newState);
        }
      }
    };

    return wc;

  }-*/;


  /**
   * For demo only purposes. An example of (stupid) Widget-Model controller. It
   * shows the state as a text with a strong background color. On click you can
   * change the state. The data model object is expected to be a String
   *
   * @return
   */
  public final static native WidgetController getDemoWidgetModelController() /*-{

    var wc = {

      onInit: function(parentElement, dataModelObject) {
        parentElement.innerHTML="<span style='background: #FFD677;'>"+dataModelObject.getValue()+"</span>";
      },

      onChangeState: function(parentElement, dataModelObject) {
        parentElement.innerHTML="<span style='background: #FFD677;'>"+dataModelObject.getValue()+"</span>";
      },

      events: {

        click: function(event, dataModelObject) {
          var newValue = prompt("Please enter new value", dataModelObject.getValue());
          dataModelObject.setValue(newValue);
        }
      }
    };

    return wc;

  }-*/;


  protected WidgetController() {

  }


  public final native JsArrayString getSupportedEvents() /*-{
    events = new Array();
    for (var field in this.events) {
      if (this.events.hasOwnProperty(field))
        events.push(field);
    }

    return events;
  }-*/;


  //
  // Basic Widgets
  //

  public final native void onInit(Element parent, String state)  /*-{
    this.onInit(parent, state);
  }-*/;

  public final native void onChangeState(Element parent, String before, String after) /*-{
    this.onChangeState(parent, before, after);
  }-*/;


  public final native void onEvent(String name, Event event, WidgetContext context) /*-{
    this.events[name](event, context);
  }-*/;


  //
  // Model Widgets
  //

  public final native void onInit(Element parent, JavaScriptObject dataModelObject)  /*-{
    this.onInit(parent, dataModelObject);
  }-*/;

  public final native void onChangeState(Element parent, JavaScriptObject dataModelObject) /*-{
    this.onChangeState(parent, dataModelObject);
  }-*/;


  public final native void onEvent(String name, Event event, JavaScriptObject dataModelObject) /*-{
    this.events[name](event, dataModelObject);
  }-*/;

}
