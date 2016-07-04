package org.swellrt.model;

import org.waveprotocol.wave.client.doodad.widget.WidgetDoodad;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

public class ModelSchemas {

  public static DocumentSchema TEXT_DOCUMENT_SCHEMA =
      new ConversationSchemas.DefaultDocumentSchema() {
        {
          addChildren("body", WidgetDoodad.TAG);
          addAttrs(WidgetDoodad.TAG, WidgetDoodad.ATTR_TYPE);
          addAttrs(WidgetDoodad.TAG, WidgetDoodad.ATTR_STATE);
          addAttrs(WidgetDoodad.TAG, WidgetDoodad.ATTR_ID);
          containsBlipText(WidgetDoodad.TAG);
          containsAnyText(WidgetDoodad.TAG);


        }
      };
}
