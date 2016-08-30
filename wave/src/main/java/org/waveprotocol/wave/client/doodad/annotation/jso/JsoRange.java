package org.waveprotocol.wave.client.doodad.annotation.jso;

import org.waveprotocol.wave.client.common.util.JsoStringMap;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;

/**
 * A single native Js class to handle different document and editor ranged stuff.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class JsoRange extends JavaScriptObject {
	
	
	public static class Builder {
		
		JsoView jso;
		JsoView annotations;
		

    CMutableDocument doc;
		int start;
		int end;
		int length;
		
		
		public static Builder create(CMutableDocument doc) {
			return new Builder(doc);
		}
		
		protected Builder(CMutableDocument doc) {
			this.jso = JsoView.create();
			this.doc = doc;
		}
		
		public Builder range(int start, int end, int length) {
			this.start = start;
			this.end = end;
			this.length = length;
			return this;
		}
		
		public Builder range(Range r) {
			return range(r.getStart(), r.getEnd(), r.getEnd() - r.getStart() + 1);
		}
		
		public Builder range(ContentElement contentElement) {
			start = contentElement.getContext().locationMapper().getLocation(contentElement);
			end = contentElement.getContext().locationMapper().getLocation(contentElement.getNextSibling());
			length = DocHelper.getItemSize(doc, contentElement);
			return this;			
		}
		

		public Builder annotation(String key, String value) {
			
			if (annotations == null) {
			  annotations = JsoView.create();
			}
	
			annotations.setString(key, value);
			return this;
		}
		

		public Builder annotations(JsoStringMap<String> map) {
		  annotations = map.backend;	
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
		
		
		public JsoRange build() {			
		  
      jso.setNumber("start", start);
      jso.setNumber("end", end);
      jso.setNumber("lenght", length);
		  
		  if (annotations != null)
		    jso.setJso("annotations", annotations);
		  
		  Node node = null;
		  Point<ContentNode> point = doc.locate(start);
		  if (point != null)
		    node = point.getCanonicalNode().getImplNodeletRightwards();	    
		    
		  jso.setJso("node", node);
		    
      
			functionize(jso, doc);
			return jso.cast();
		}
		
		private native void functionize(JavaScriptObject jso, CMutableDocument doc) /*-{
			if (jso != null && typeof jso.start == "number" && typeof jso.end == "number")
				jso.text = @org.waveprotocol.wave.client.doodad.annotation.jso.JsoRange::getRangeText(IILorg/waveprotocol/wave/client/editor/content/CMutableDocument;)(jso.start, jso.end, doc);
			else
				jso = "";			
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
	
	public final StringMap<String> getAnnotations() {
	  StringMap<String> map = CollectionUtils.<String>createStringMap();
	  JsoView annotations = getAnnotationsJsoView();
		
	  annotations.each(new ProcV<String>(){

      @Override
      public void apply(String key, String value) {
        map.put(key, value);       
      }
	    
	  });
	  
		return map;
	}
	 
	private static String getRangeText(int start, int end, CMutableDocument doc) {
		return DocHelper.getText(doc, start, end);
	}
	
	private static void deleteRangeText(int start, int end, CMutableDocument doc) {
		doc.deleteRange(start, end);
	}
			
	protected JsoRange() {
		
	}

}
