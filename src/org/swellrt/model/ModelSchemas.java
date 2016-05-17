package org.swellrt.model;


import org.swellrt.model.doodad.WidgetDoodad;
import org.swellrt.model.doodad.WidgetModelDoodad;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

public class ModelSchemas {

  public static DocumentSchema TEXT_DOCUMENT_SCHEMA =
      new ConversationSchemas.DefaultDocumentSchema() {
        {
          addChildren("body", WidgetDoodad.TAG);
          addAttrs(WidgetDoodad.TAG, WidgetDoodad.ATTR_TYPE);
          addAttrs(WidgetDoodad.TAG, WidgetDoodad.ATTR_STATE);
          containsBlipText(WidgetDoodad.TAG);
          containsAnyText(WidgetDoodad.TAG);

          addChildren("body", WidgetModelDoodad.TAG);
          addAttrs(WidgetModelDoodad.TAG, WidgetModelDoodad.ATTR_TYPE);
          addAttrs(WidgetModelDoodad.TAG, WidgetModelDoodad.ATTR_PATH);
        }
      };
}
