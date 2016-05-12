package org.swellrt.client.editor;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * A wrapper native class to interact with documents selections.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class TextEditorSelection extends JavaScriptObject {


  public static native TextEditorSelection create(Range range, CMutableDocument doc) /*-{

    var selection = {

      toString: function() {
          return @org.swellrt.client.editor.TextEditorSelection::toString(Lorg/waveprotocol/wave/model/document/util/Range;Lorg/waveprotocol/wave/client/editor/content/CMutableDocument;)(range, doc);
      },

      remove: function() {
          @org.swellrt.client.editor.TextEditorSelection::deleteRange(Lorg/waveprotocol/wave/model/document/util/Range;Lorg/waveprotocol/wave/client/editor/content/CMutableDocument;)(range, doc);
      }

    };

    return selection;

  }-*/;

  protected TextEditorSelection() {

  }

  public static String toString(Range range, CMutableDocument doc) {
    return DocHelper.getText(doc, range.getStart(), range.getEnd());
  }

  public static void deleteRange(Range range, CMutableDocument doc) {
    doc.deleteRange(range.getStart(), range.getEnd());
  }

}
