package org.swellrt.snippets.editor;

import java.util.Map;

import org.swellrt.beta.client.js.editor.SEditorConfig;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class EditorComponent extends Composite {


  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";

  static {
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);
  }

  // Logger
  LoggerBundle logger = new DomLogger("test");

  ContentDocument doc1;
  Editor editor1;


  ContentDocument doc2;
  Editor editor2;
  DiffHighlightingFilter diffDoc2;

  // Panel to render the document with no editor
  LogicalPanel.Impl displayDoc2 = new LogicalPanel.Impl() {
    {
      setElement(Document.get().createDivElement());
      getElement().getStyle().setProperty("border", "1px solid black");
    }
  };



  // Init Editor registries
  static {
    Editors.initRootRegistries();
  }

  public void registerDoodads(Registries registries) {


    LineRendering.registerContainer(TOPLEVEL_CONTAINER_TAGNAME,
        registries.getElementHandlerRegistry());

    StyleAnnotationHandler.register(registries);
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

    // add more doodads
  }

  private Editor createEditor(String id) {
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
      public PopupChrome createPopupChrome() {
        return null;
      }
    });

    Editor editor = Editors.create();
    editor.getWidget().getElement().setId(id);
    // editor.addKeySignalListener(this);
    return editor;
  }

  private DocumentSchema getSchema() {
    return ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS;
  }

  private Registries getRegistries() {
    return Editor.ROOT_REGISTRIES;
  }

  protected EditorSettings getSettings() {

    return new EditorSettings()
        .setHasDebugDialog(true)
        .setUndoEnabled(false)
        .setUseFancyCursorBias(true)
        .setUseSemanticCopyPaste(false)
        .setUseWhitelistInEditor(false)
        .setUseWebkitCompositionEvents(false);

  }

  public void init() {

    try {

      // EditorWebDriverUtil.setDocumentSchema(ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);
      registerDoodads(getRegistries());

      //
      // Editor 1
      //

      editor1 = createEditor("editor1");
      editor1.init(getRegistries(), new KeyBindingRegistry(), getSettings());
      doc1 = createContentDocument("This is doc", false);
      editor1.setContent(doc1);
      editor1.setEditing(true);

      boolean showDiffs = true;
      editor2 = createEditor("editor2");
      editor2.init(getRegistries(), new KeyBindingRegistry(), getSettings());
      doc2 = createContentDocument("This is doc", false);
      diffDoc2 = new DiffHighlightingFilter(doc2.getDiffTarget());
      editor2.setContent(doc2);
      editor2.setEditing(false);

      // Render a content document in a panel
      /*
       * editorPanel.add(displayDoc2); doc2 =
       * createContentDocument("This is doc two", false); doc2.setRendering();
       * displayDoc2.getElement()
       * .appendChild(doc2.getFullContentView().getDocumentElement().
       * getImplNodelet());
       */

      // Mirror changes in editor1 to doc2
      editor1.setOutputSink(new SilentOperationSink<DocOp>() {
        @Override
        public void consume(DocOp op) {
          if (showDiffs) {
            try {
              diffDoc2.consume(op);
            } catch (OperationException e) {
              GWT.log("Error consuming op", e);
            }
          } else {
            doc2.consume(op);
          }
        }

      });

      //
      // Attach to HTML
      //

      VerticalPanel editorPanel = new VerticalPanel();
      editorPanel.add(editor1.getWidget());
      editorPanel.add(new HTML("<div style='border-bottom: 1px solid black'></div>"));
      editorPanel.add(editor2.getWidget());

      initWidget(editorPanel);


    } catch (RuntimeException ex) {
      GWT.log("Fatal Exception", ex);
    }
  }

  private ContentDocument createContentDocument(String initContent, boolean validateSchema) {

    DocInitialization op;
    try {
      op = DocProviders.POJO.parse(initContent).asOperation();
    } catch (IllegalArgumentException e) {
      GWT.log("Exception processing content", e);
      return null;
    }

    DocumentSchema schema = DocumentSchema.NO_SCHEMA_CONSTRAINTS;

    if (validateSchema) {
      schema = getSchema();
      ViolationCollector vc = new ViolationCollector();
      if (!DocOpValidator.validate(vc, getSchema(), op).isValid()) {
        GWT.log("Error validating content " + vc.firstDescription());
      }
    }

    return new ContentDocument(getRegistries(), op, schema);

  }

  public Editor getEditor() {
    return editor1;
  }

}
