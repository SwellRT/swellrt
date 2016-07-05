package org.waveprotocol.wave.client.doodad.annotation;

	import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.util.DocHelper;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A native Javascript wrapper to handle annotated text content
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AnnotationContent extends JavaScriptObject {
	
	
	private static void setContent(ContentElement contentElement, String annotationKey, String text) {
		


	}
	
	private static String getContent(ContentElement contentElement, String annotationKey) {
		/*
		if (contentElement != null) {
			ContentNode node = contentElement.getFirstChild();
			if (node != null && node.asText() != null) 
				return node.asText().getData();			
		}
		*/
			
		if (contentElement != null) {
			ContentNode node = contentElement.getFirstChild();
			if (node != null && node.asText() != null) {
				String nodeText = node.asText().getData();
				CMutableDocument doc = contentElement.getMutableDoc();
				int startLocation = contentElement.getContext().locationMapper().getLocation(contentElement);
				int endLocation = contentElement.getContext().locationMapper().getLocation(contentElement.getNextSibling());
				return nodeText+" = "+DocHelper.getText(doc, startLocation, endLocation);
				
			}
		}	
		
		return null;
	}
	
	public static native AnnotationContent get(ContentElement contentElement, String annotationKey) /*-{
	
		return {
		
			setContent: function(text) {
						
			},
			
			getContent: function() {
			   return @org.waveprotocol.wave.client.doodad.annotation.AnnotationContent::getContent(Lorg/waveprotocol/wave/client/editor/content/ContentElement;Ljava/lang/String;)(contentElement, annotationKey);
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
