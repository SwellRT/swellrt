package org.swellrt.beta.client.js.editor.annotation;

import java.util.Map;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Paragraph annotation type. See internals in {@See Paragraph}.
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ParagraphActionAnnotation implements Annotation {
  
  private final Map<String, ContentElement.Action> actions;

  
  public ParagraphActionAnnotation(Map<String, ContentElement.Action> actions, ContentElement.Action resetAction) {
    this.actions = actions;
    this.actions.put("reset", resetAction);
  }
  
  @Override
  public void set(EditorContext editor, Range range, String actionName) {
    if (range != null && editor != null && actionName != null) {
      
      final ContentElement.Action action = actions.get(actionName);
      if (action == null)
        return;

      editor.undoableSequence(new Runnable(){
        @Override public void run() {
          LocationMapper<ContentNode> locator = editor.getDocument();
          Paragraph.traverse(locator, range.getStart(), range.getEnd(), action);
        }
      });

    }
  }

  @Override
  public void reset(EditorContext editor, Range range) {
    set(editor, range, "reset");
  }

}
