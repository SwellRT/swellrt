package org.waveprotocol.wave.client.doodad.annotation;

import org.waveprotocol.wave.client.editor.content.ContentElement;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A native Javascript wrapper to handle with annotated text content
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AnnotationContent extends JavaScriptObject {
	
	public static native AnnotationContent get(ContentElement contentElement) /*-{
	
		return {
		
			setContent: function(text) {
			
			
			},
			
			getContent: function() {
			
			
			},
			
			setAnnotation: function(value) {
			
			
			},
			
			removeAnnotation: function() {
			
			
			}
		
		
		};
	
	}-*/;
	
	protected AnnotationContent() {
		
	}
	
	

}
