package org.waveprotocol.wave.client.doodad.annotation.jso;

import org.waveprotocol.wave.client.editor.content.CMutableDocument;

import com.google.gwt.dom.client.Element;

public class JsoParagraphAnnotation extends JsoAnnotation {
	
	public static native JsoParagraphAnnotation create(CMutableDocument doc, int _start, int _end, String _key, String _value, Element _element) /*-{
	
		var jso = {
		
			key: _key,
			
			id: _element.@com.google.gwt.dom.client.Element::getId()(),
						
			value: _value, 
							
			text: _element.@com.google.gwt.dom.client.Element::getInnerText()(),
						
			element: _element,
			
			start : _start,
			
			end : _end
	
		};
	
		return jso;
	
	}-*/;
	

	protected JsoParagraphAnnotation() {		
	}
	

	
}
