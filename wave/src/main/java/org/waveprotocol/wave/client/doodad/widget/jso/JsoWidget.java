package org.waveprotocol.wave.client.doodad.widget.jso;

import org.waveprotocol.wave.client.doodad.widget.WidgetDoodad;
import org.waveprotocol.wave.client.editor.content.ContentElement;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

/**
 * A native JavaScript view of a Widget
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class JsoWidget extends JavaScriptObject {
	
	
	public static native JsoWidget create(Element domElement, ContentElement contentElement) /*-{
	
		var jso = {
		
			getElement: function() {
				return domElement;
			},
		
			setState: function(state) {			
				@org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget::setState(Lorg/waveprotocol/wave/client/editor/content/ContentElement;Ljava/lang/String;)(contentElement, state);
			},
			
			getState: function() {
				return @org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget::getState(Lorg/waveprotocol/wave/client/editor/content/ContentElement;)(contentElement);
			},

			getType: function() {
				return @org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget::getType(Lorg/waveprotocol/wave/client/editor/content/ContentElement;)(contentElement);
			},
			
			getId: function() {
				return @org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget::getId(Lorg/waveprotocol/wave/client/editor/content/ContentElement;)(contentElement);
			},
			
			remove: function() {
				@org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget::remove(Lorg/waveprotocol/wave/client/editor/content/ContentElement;)(contentElement);
			},
			
			isOk: function() {
				return @org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget::isOk(Lorg/waveprotocol/wave/client/editor/content/ContentElement;)(contentElement);
			}
			
		};
		
		return jso;
	
	}-*/;
	
	protected JsoWidget() {
		
	}
	
	private static void setState(ContentElement contentElement, String state) {
		contentElement.getContext().document().setElementAttribute(contentElement, WidgetDoodad.ATTR_STATE, state);
	}
	
	private static String getState(ContentElement contentElement) {
		return contentElement.getContext().document().getAttribute(contentElement, WidgetDoodad.ATTR_STATE);
	}
	
	private static String getType(ContentElement contentElement) {
		return contentElement.getContext().document().getAttribute(contentElement, WidgetDoodad.ATTR_TYPE);
	}

	private static String getId(ContentElement contentElement) {	
		return contentElement.getContext().document().getAttribute(contentElement, WidgetDoodad.ATTR_ID);
	}
	
	private static boolean isOk(ContentElement contentElement) {
		return contentElement != null && contentElement.isConsistent() && contentElement.isContentAttached();
	}
	
	private static void remove(ContentElement contentElement) {
		contentElement.getContext().document().deleteNode(contentElement);
	}
}
