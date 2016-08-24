package org.waveprotocol.wave.client.doodad.annotation.jso;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A native Js view of an annotation.
 * 
 * @author pablojan
 *
 */
public class JsoAnnotation extends JavaScriptObject {
	
	public static native JsoAnnotation create(Editor editor, Range range, String _key) /*-{
	
		var jso = {
		
			key: _key, 
			
			id: null, 
						
			value: @org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotation::getValue(Lorg/waveprotocol/wave/client/editor/Editor;Lorg/waveprotocol/wave/model/document/util/Range;Ljava/lang/String;)(editor, range, _key),
		
			text:  @org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotation::getText(Lorg/waveprotocol/wave/client/editor/Editor;Lorg/waveprotocol/wave/model/document/util/Range;)(editor, range),
		
			start : range.@org.waveprotocol.wave.model.document.util.Range::getStart()(),
			
			end : range.@org.waveprotocol.wave.model.document.util.Range::getEnd()(),
			
			element : null

		};

		return jso;
	
	}-*/;
	
	protected JsoAnnotation() {
		
	}
	
	protected static String getValue(Editor editor, Range range, String key) {
		return EditorAnnotationUtil.getAnnotationOverRangeIfFull(editor.getDocument(), editor.getCaretAnnotations(), key, range.getStart(), range.getEnd());
	}

	protected static String getText(Editor editor, Range range) {
		return DocHelper.getText(editor.getDocument(), range.getStart(), range.getEnd());
	}

	

}
