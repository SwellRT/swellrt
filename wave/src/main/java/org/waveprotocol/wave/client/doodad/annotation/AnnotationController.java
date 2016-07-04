package org.waveprotocol.wave.client.doodad.annotation;


import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.widget.WidgetController;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
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
	
	
	public final static StringMap<AnnotationController> fromJso(JavaScriptObject controllers) {

		final StringMap<AnnotationController> map = CollectionUtils.createStringMap();
		
		if (controllers != null) {
			JsoView jsoView = JsoView.as(controllers);
			jsoView.each(new ProcV<AnnotationController>() {
	
				@Override
				public void apply(String key, AnnotationController value) {
					map.put(key, value);
				}
	
			});
		}
		return map;
	}
	
	/**
	 * For debug purpose only.
	 */
	public native static AnnotationController getDefault() /*-{
	
		return {
		
			onEvent: function(annotationContent, event) {

			},
			
			onChange: function(annotationContent) {
		
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
	public native final JsArrayString getStyleNames() /*-{
		if (this.styles)
			return Object.getOwnPropertyNames(this.styles);
			
		return new Array();
	}-*/;
	
	/**
	 * Handle events raised in the rendered annotation element. 
	 */
	public native final void onEvent(AnnotationContent annotationContent, Event event) /*-{
		if (this.onEvent) 
			this.onEvent(annotationContent, event);
	}-*/;
	
	public native final void onChange(AnnotationContent annotationContent) /*-{
		if (this.onChange)
			this.onChange(annotationContent);
	}-*/;
	

}
