package org.swellrt.client.editor;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.swellrt.client.editor.doodad.ExternalAnnotationHandler;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.document.util.LineContainers;
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

  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";

  static {
    Editors.initRootRegistries();
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);
  }


  private static final EditorSettings EDITOR_SETTINGS = new EditorSettings()
      .setHasDebugDialog(true).setUndoEnabled(true).setUseFancyCursorBias(true)
      .setUseSemanticCopyPaste(false).setUseWhitelistInEditor(false)
      .setUseWebkitCompositionEvents(true);

  // Wave Editor specific

  private final Registries registries = Editor.ROOT_REGISTRIES;
  private final KeyBindingRegistry KEY_REGISTRY = new KeyBindingRegistry();


  private LogicalPanel.Impl editorPanel;
  private LogicalPanel.Impl docPanel;

  /**
   * The gateway to get UI-versions of Blips. Registry is GWT related, so must
   * be injected in the Editor by the JS API. Model's classes have to ignore it.
   */
  private WaveDocuments<? extends InteractiveDocument> documentRegistry;

  private Editor editor;
  private ContentDocument doc;

  public static TextEditor create() {
    TextEditor editor = new TextEditor();
    return editor;
  }

  protected TextEditor() {
  }

  /**
   * Inject document registry which manages UI versions of blips. Registry must
   * be only injected by the JS API.
   *
   * @param documentRegistry
   */
  public void setDocumentRegistry(WaveDocuments<? extends InteractiveDocument> documentRegistry) {
    this.documentRegistry = documentRegistry;
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
      editor.init(null, KEY_REGISTRY, EditorSettings.DEFAULT);

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
      editor.init(null, KEY_REGISTRY, EDITOR_SETTINGS);

    }

    // editor.getWidget().getElement().setId(id);
    // editor.addKeySignalListener(parentPanel);


  }

  private void setText(TextType text) {

    Preconditions.checkNotNull(documentRegistry,
        "A document registry must be provided before editing a text");

    doc =
        documentRegistry
            .getBlipDocument(text.getModel().getWaveletIdString(), text.getDocumentId())
            .getDocument();

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


    // TOPLEVEL_CONTAINER_TAGNAME
    LineRendering.registerContainer(TOPLEVEL_CONTAINER_TAGNAME,
        registries.getElementHandlerRegistry());

    StyleAnnotationHandler.register(registries);

    // Listen for Diff annotations to paint new content or to insert a
    // delete-content tag
    // to be rendered by the DiffDeleteRendere
    DiffAnnotationHandler.register(registries.getAnnotationHandlerRegistry(),
        registries.getPaintRegistry());
    DiffDeleteRenderer.register(registries.getElementHandlerRegistry());

    LinkAnnotationHandler.register(registries, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });

    ExternalAnnotationHandler.register(registries);

    // Add additional doodas here
  }


}
