package org.waveprotocol.wave.client.doodad.annotation.jso;

import org.waveprotocol.wave.client.common.util.JsoStringMap;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A single native Js class to handle different document and editor ranged stuff.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class JsoEditorRange extends JavaScriptObject {
	
	
	public static class Builder {
		
		JsoView jso;
		CMutableDocument doc;
		
		public static Builder create(CMutableDocument doc) {
			return new Builder(doc);
		}
		
		protected Builder(CMutableDocument doc) {
			this.jso = JsoView.create();
			this.doc = doc;
		}
		
		public Builder range(int start, int end, int length) {
			jso.setNumber("start", start);
			jso.setNumber("end", end);
			jso.setNumber("lenght", length);
			return this;
		}
		
		public Builder range(Range r) {
			return range(r.getStart(), r.getEnd(), r.getEnd() - r.getStart() + 1);
		}
		
		public Builder range(ContentElement contentElement) {
			int start = contentElement.getContext().locationMapper().getLocation(contentElement);
			int end = contentElement.getContext().locationMapper().getLocation(contentElement.getNextSibling());
			int lenght = DocHelper.getItemSize(doc, contentElement);
			return range(start, end, lenght);			
		}
		

		public Builder annotation(String key, String value) {
			
			JsoView annotations;
			
			if (jso.getJso("annotations") == null) {
				jso.setJso("annotations", JsoView.create());
			}
			annotations = jso.getJsoView("annotations");
			annotations.setString(key, value);
			return this;
		}
		

		public Builder annotations(JsoStringMap<String> map) {
			jso.setJso("annotations", map.backend);			
			return this;
		}
		
		public Builder annotationInterval(AnnotationInterval<String> interval) {
			
			range(interval.start(), interval.end(), interval.length());
			interval.annotations().each(new ProcV<String>() {

				@Override
				public void apply(String key, String value) {
					annotation(key, value);				
				}
				
			});
			return this;
		}
		
		
		public JsoEditorRange build() {			
			functionize(jso, doc);
			return jso.cast();
		}
		
		private native void functionize(JavaScriptObject jso, CMutableDocument doc) /*-{
		
			jso.getText = function() {
				return @org.waveprotocol.wave.client.doodad.annotation.jso.JsoEditorRange::getRangeText(IILorg/waveprotocol/wave/client/editor/content/CMutableDocument;)(jso.start, jso.end, doc);
			};
			
			jso.remove = function() {
				@org.waveprotocol.wave.client.doodad.annotation.jso.JsoEditorRange::deleteRangeText(IILorg/waveprotocol/wave/client/editor/content/CMutableDocument;)(jso.start, jso.end, doc);
			};
			
			
		}-*/;
		
	}
	
	public final native int start() /*-{
		return this.start;
	}-*/;

	public final native int end() /*-{
		return this.end;
	}-*/;
	
	private final native JsoView getAnnotationsJsoView() /*-{
		return this.annotations;
	}-*/;
	
	public final String[] getAnnotationKeys() {
		JsoView jsoKeys = getAnnotationsJsoView();
		String[] keys = new String[jsoKeys.countEntries()];
		jsoKeys.each(new ProcV<Object>() {
			int c = 0;
			@Override
			public void apply(String key, Object value) {
				keys[c++] = key;			
			}
			
		});
		return keys;
	}
	 
	private static String getRangeText(int start, int end, CMutableDocument doc) {
		return DocHelper.getText(doc, start, end);
	}
	
	private static void deleteRangeText(int start, int end, CMutableDocument doc) {
		doc.deleteRange(start, end);
	}
			
	protected JsoEditorRange() {
		
	}

}
