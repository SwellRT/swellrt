package org.waveprotocol.wave.client.doodad.annotation;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

/**
 * 
 * A native Javascript wrapper to configure externally annotations from browser's code.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AnnotationController extends JavaScriptObject {
	
	/**
	 * For debug purpose only.
	 */
	public native static AnnotationController getDefault() /*-{
	
		return {
		
			onEvent: function(element, event) {
				//console.log("On Annotation Event "+event); 
			},
			
			onChange: function(element) {
				console.log("On Annotation Change "+element);			
			},
			
			styleClass: "default-annotation",
			
			styles: {
				"text-decoration" : "underline red",
				"font-style" : "italic"
			}					
		};
		
	 
	 }-*/;
	
	
	protected AnnotationController() {}
	
	public native final String getCSSClass() /*-{
		return this.styleClass;
	}-*/;
	
	public native final String getStyleValue(String styleName) /*-{
			if (this.styles)
				return this.styles[styleName];
			
			return null;
	}-*/;
	
	@SuppressWarnings("rawtypes")
	public native final JsArray getStyleNames() /*-{
		if (this.styles)
			return Object.getOwnPropertyNames(this.styles);
			
		return new Array();
	}-*/;
	
	/**
	 * Handle events raised in the rendered annotation element. 
	 */
	public native final void onEvent(Element element, Event event) /*-{
		if (this.onEvent) 
			this.onEvent(element, event);
	}-*/;
	
	public native final void onChange(Element element) /*-{
		if (this.onChange)
			this.onChange(element);
	}-*/;
	

}
