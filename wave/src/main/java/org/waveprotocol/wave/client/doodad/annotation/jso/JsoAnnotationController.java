package org.waveprotocol.wave.client.doodad.annotation.jso;


import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Event;

/**
 * 
 * A native Javascript wrapper to configure externally annotations from browser's code.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class JsoAnnotationController extends JavaScriptObject {
	
	
	public final static StringMap<JsoAnnotationController> fromJso(JavaScriptObject controllers) {

		final StringMap<JsoAnnotationController> map = CollectionUtils.createStringMap();
		
		if (controllers != null) {
			JsoView jsoView = JsoView.as(controllers);
			jsoView.each(new ProcV<JsoAnnotationController>() {
	
				@Override
				public void apply(String key, JsoAnnotationController value) {
					map.put(key, value);
				}
	
			});
		}
		return map;
	}
	
	/**
	 * For debug purpose only.
	 */
	public native static JsoAnnotationController getDefault() /*-{
	
		return {
		
			onEvent: function(annotationContent, event) {
				console.log("On Event Generic annotation"); 
				$wnd.lastEvent = event;
				$wnd.lastEventAnnotation = annotationContent;
			},
			
			onChange: function(annotationContent) {
				console.log("On Mutation Generic annotation"); 
				$wnd.lastAnnotationChange = annotationContent;
			},
			
			styleClass: "default-annotation",
			
			style: {
				"textDecoration" : "underline",
				"fontStyle" : "italic",
				"color" : "red"
			}					
		};
		
	 
	 }-*/;
	
	
	protected JsoAnnotationController() {}
	
	public native final String getStyleClass() /*-{
		return this.styleClass;
	}-*/;
	
	public native final String getStyleValue(String styleName) /*-{
			if (this.style)
				return this.style[styleName];
			
			return null;
	}-*/;
	
	public native final JsArrayString getStyleNames() /*-{
		if (this.style)
			return Object.getOwnPropertyNames(this.style);
			
		return new Array();
	}-*/;
	
	public native final JsoView getStylesInline() /*-{
	  if (this.style)
	    return this.style;
	}-*/;
	
	/**
	 * Handle events raised in the rendered annotation element. 
	 */
	public native final void onEvent(JsoEditorRange annotationContent, Event event) /*-{
		if (this.onEvent) 
			this.onEvent(annotationContent, event);
	}-*/;
	
	public native final void onChange(JsoEditorRange annotationContent) /*-{
		if (this.onChange)
			this.onChange(annotationContent);
	}-*/;
	

}
