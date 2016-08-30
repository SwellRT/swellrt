package org.waveprotocol.wave.client.doodad.widget.jso;

import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;

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
public class JsoWidgetController extends JavaScriptObject {


  public final static StringMap<JsoWidgetController> fromJso(JavaScriptObject controllers) {
	  
	  final StringMap<JsoWidgetController> map = CollectionUtils.createStringMap();
	  if (controllers != null) {
		  JsoView jsoView = JsoView.as(controllers);
		  jsoView.each(new ProcV<JsoWidgetController>() {
	
			@Override
			public void apply(String key, JsoWidgetController value) {
				map.put(key, value);			
			}
			  
		  });
	  }
	  return map;
  }
	
  /**
   * For demo only purposes. An example of (stupid) Widget controller. It shows
   * the state as a text with a strong background color. On click you can change
   * the state.
   *
   * @return
   */
  public final static native JsoWidgetController getDemoWidgetController() /*-{

    var jso = {

      onInit: function(element, state) {
        element.innerHTML="<span style='background: #FFD677;'>"+state+"</span>";
      },

      onChangeState: function(element, before, after) {
        element.innerHTML="<span style='background: #FFD677;'>"+after+"</span>";
      },

  	  onActivated: function(element) {
  	   	// attach event handlers
  	  },
  	  
  	  onDeactivated: function(element) {
  	  	// deattach event handlers
  	  }

    };

    return jso;

  }-*/;



  protected JsoWidgetController() {
  }


  public final native void onInit(Element parent, String state)  /*-{
  	if (this.onInit && typeof this.onInit == 'function')
    	this.onInit(parent, state);
  }-*/;

  public final native void onChangeState(Element parent, String oldState, String newState) /*-{
  	if (this.onChangeState && typeof this.onChangeState == 'function')
    	this.onChangeState(parent, oldState, newState);
  }-*/;
  
  public final native void onActivated(Element parent) /*-{
    if (this.onActivated && typeof this.onActivated == 'function')
    	this.onActivated(parent);
  }-*/;
  
  public final native void onDeactivated(Element parent) /*-{
      if (this.onDeactivated && typeof this.onDeactivated == 'function')
    	this.onDeactivated(parent);  
  }-*/;

  /**
   * Sets the reference to the editor's pure javascript facade.
   * Allows handlers to interact with the editor.
   * 
   * @param jsEditorFacade
   */
  public final native void setEditorJsFacade(JavaScriptObject jsEditorFacade) /*-{
    this.editor = jsEditorFacade;
  }-*/;

}
