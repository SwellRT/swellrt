package org.swellrt.client.editor;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

import java.util.Map;

/**
 * A wrapper of the original Wave {@link Editor} to be integrated in the SwellRT
 * Web API. Users can edit TextType instances in this editor.
 *
 * @author pablojan
 *
 */
public class TextEditor {


  // Wave Editor specific

  private final Registries registries = Editor.ROOT_REGISTRIES;
  private final KeyBindingRegistry keysRegistry = new KeyBindingRegistry();


  private LogicalPanel.Impl editorPanel;
  private LogicalPanel.Impl docPanel;

  private Editor editor;
  private ContentDocument doc;

  public static TextEditor create() {
    TextEditor editor = new TextEditor();
    return editor;
  }

  protected TextEditor() {
  }


  private void launchEditor(ContentDocument doc) {

    if (editor == null) {

      // First time - create and init new editor.
      EditorWebDriverUtil.setDocumentSchema(ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);
      registerDoodads();

      EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
      EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
        public PopupChrome createPopupChrome() {
          return null;
        }
      });

      editor = Editors.attachTo(doc);
      editor.init(null, keysRegistry, EditorSettings.DEFAULT);

      editor.addUpdateListener(new EditorUpdateListener() {
        @Override
        public void onUpdate(EditorUpdateEvent event) {

        }
      });

    } else {

      // Reuse exsiting editor.
      // editor.removeContent();
      if (editor.hasDocument()) {
        editor.removeContentAndUnrender();
        editor.reset();
      }
      editor.setContent(doc);
      editor.init(null, keysRegistry, EditorSettings.DEFAULT);

    }

    // editor.getWidget().getElement().setId(id);
    // editor.addKeySignalListener(parentPanel);


  }

  private void setText(TextType text) {

    doc = text.getDocument();
    Preconditions.checkArgument(doc != null, "Can't edit an unattached TextType");

    this.docPanel = new LogicalPanel.Impl() {
      {
        setElement(Document.get().createDivElement());
      }
    };

    doc.setInteractive(docPanel);

    // Append the doc panel to the provided container panel
    editorPanel.getElement().appendChild(
        doc.getFullContentView().getDocumentElement().getImplNodelet());

  }


  public void setElement(String elementId) {
    final Element element = Document.get().getElementById(elementId);

    if (element == null) return;

    this.editorPanel = new LogicalPanel.Impl() {
      {
        setElement(element);
      }
    };
  }

  public void edit(TextType text) {
    Preconditions.checkNotNull(editorPanel, "Panel not set for TextEditor");
    setText(text);
    Preconditions.checkNotNull(this.doc, "ContentDocument can't be null");
    launchEditor(doc);
    editor.setEditing(true);
    editor.focus(true);
  }

  public void setEditing(boolean isEditing) {
    if (editor.hasDocument()) {
      if (editor.isEditing() != isEditing) editor.setEditing(isEditing);
    }
  }

  public void cleanUp() {
    if (editor != null) {
      editor.setEditing(false);
      doc = null;
      editor.removeContentAndUnrender();
      editor.reset();
    }
  }



  private void registerDoodads() {

    ElementHandlerRegistry testHandlerRegistry = registries.getElementHandlerRegistry();

    // TOPLEVEL_CONTAINER_TAGNAME
    LineRendering.registerContainer("body",
        registries.getElementHandlerRegistry());

    StyleAnnotationHandler.register(registries);
    DiffAnnotationHandler.register(registries.getAnnotationHandlerRegistry(),
        registries.getPaintRegistry());

    LinkAnnotationHandler.register(registries, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });

    // TODO(danilatos): Open source spelly stuff
    // SpellDocument testSpellDocument =
    // SpellDebugHelper.createTestSpellDocument(
    // EditorStaticDeps.logger);
    // SpellAnnotationHandler.register(Editor.ROOT_REGISTRIES,
    // SpellySettings.DEFAULT, testSpellDocument);
    // SpellDebugHelper.setDebugSpellDoc(testSpellDocument);
    // SpellSuggestion.register(testHandlerRegistry, testSpellDocument);
    // SpellTesting.registerDebugCombo(keysRegistry);
    // SpellDebugHelper.setDebugSpellDoc(testSpellDocument);

    // Add additional doodas here
  }


}
