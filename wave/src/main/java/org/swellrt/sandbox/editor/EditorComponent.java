package org.swellrt.sandbox.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.playback.DocOpContext;
import org.waveprotocol.wave.client.editor.playback.DocOpContextCache;
import org.waveprotocol.wave.client.editor.playback.FakeDocHistory;
import org.waveprotocol.wave.client.editor.playback.PlaybackDocument;
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
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class EditorComponent extends Composite {

  public static String toCamel(String cssName) {
    String[] parts = cssName.split("-");
    if (parts.length == 0)
      return cssName;

    String camelName = parts[0];

    for (int i = 1; i < parts.length; i++) {
      camelName += parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1);
    }

    return camelName;
  }

  public static void setCSS(Widget w, String... styles) {
    if (styles.length < 2)
      return;

    for (int i = 0; i < styles.length; i = i + 2) {
      w.getElement().getStyle().setProperty(toCamel(styles[i]), styles[i + 1]);
    }

  }


  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";
  private static final DocumentSchema DOC_SCHEMA = DocumentSchema.NO_SCHEMA_CONSTRAINTS;

  static {
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);
  }

  // Logger
  LoggerBundle logger = new DomLogger("test");

  final ParticipantId DUMMY_PARTICIPANT = ParticipantId.ofUnsafe("dummy@dummy.org");

  // Editor 1

  ContentDocument doc1;
  Editor editor1;
  List<DocOp> doc1ops = new ArrayList<DocOp>();


  // Editor 2

  /** The ops history of doc 1 to be replayed in doc2 */
  FakeDocHistory docHistory = new FakeDocHistory();

  /** A doc op cache to query op metadata from ops consumers */
  DocOpContextCache docOpCache = new DocOpContextCache() {

    Map<DocOp, DocOpContext> cacheData = new HashMap<DocOp, DocOpContext>();

    @Override
    public Optional<DocOpContext> fetch(DocOp op) {
      return Optional.ofNullable(cacheData.get(op));
    }

    @Override
    public void add(DocOp op, DocOpContext opCtx) {
      cacheData.put(op, opCtx);
    }
  };

  Editor editor2;
  /** a document that can play back and forth its ops */
  PlaybackDocument doc2;

  final static String REPLAY_FLOW = "flow"; // default
  final static String REPLAY_BATCH = "batch";

  String replayMode = REPLAY_FLOW;

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
        .setUseFancyCursorBias(false)
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
      doc1 = createContentDocument(false);
      editor1.setContent(doc1);
      editor1.setEditing(true);

      boolean showDiffs = true;
      editor2 = createEditor("editor2");
      editor2.init(getRegistries(), new KeyBindingRegistry(), getSettings());
      doc2 =  new PlaybackDocument(getRegistries(), DOC_SCHEMA, docHistory, docOpCache);
      editor2.setContent(doc2.getDocument());
      doc2.renderDiffs(true);
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
      setSinks();

      //
      // Attach to HTML
      //

      HorizontalPanel mainPanel = new HorizontalPanel();
      mainPanel.setWidth("100%");

      mainPanel.add(uiEditor1());
      mainPanel.setCellWidth(uiEditor1(), "50%");

      mainPanel.add(uiEditor2());
      mainPanel.setCellWidth(uiEditor2(), "50%");


      initWidget(mainPanel);


    } catch (RuntimeException ex) {
      GWT.log("Fatal Exception", ex);
    }
  }

  private long opCounter = 0;

  private void setSinks() {

    editor1.setOutputSink(new SilentOperationSink<DocOp>() {
      @Override
      public void consume(DocOp op) {

        opCounter++;

        if (replayMode.equals(REPLAY_BATCH)) {

          doc1ops.add(op);

        } else if (replayMode.equals(REPLAY_FLOW)) {

          docHistory.addSingleDelta(DUMMY_PARTICIPANT, op);

        }

        updateInfo1();

      }

    });
  }

  private ContentDocument createContentDocument(boolean validateSchema) {

    DocInitialization op;
    try {
      op = DocProviders.POJO.parse("").asOperation();
    } catch (IllegalArgumentException e) {
      GWT.log("Exception processing content", e);
      return null;
    }



    if (validateSchema) {
      ViolationCollector vc = new ViolationCollector();
      if (!DocOpValidator.validate(vc, getSchema(), op).isValid()) {
        GWT.log("Error validating content " + vc.firstDescription());
      }
    }
    return new ContentDocument(getRegistries(), op, DOC_SCHEMA);

  }

  public Editor getEditor() {
    return editor1;
  }

  //
  // UI Behavior
  //

  boolean groupOps = false;

  public void toggleOpGrouping() {
    // Operation grouping doesn't work with
    // the EditorOperationSequencer for the CMutableDocument

    if (!groupOps) {
      groupOps = true;
      doc1.getMutableDoc().beginMutationGroup();
      btnOpGroup.setText("End Ops. Group");
    } else {
      groupOps = false;
      doc1.getMutableDoc().endMutationGroup();
      btnOpGroup.setText("Begin Ops. Group");
    }
  }



  /*
   * Load doc ops generated in doc1 into the doc2 using a DocHistory. Replay is
   * accumulative, every call adds new ops to doc2
   */
  public void replay() {

    // take ops from doc1 to play them in doc2/editor2
    // docHistory.addSingleDelta(ParticipantId.ofUnsafe("dummy@swellrt.org"), doc1ops);
    docHistory.addAllDeltas(DUMMY_PARTICIPANT, doc1ops);
    doc1ops.clear();
  }


  //
  // UI
  //

  VerticalPanel vpEditor1;
  HorizontalPanel ctrlPanel1;
  Label lblOpsCounter;
  TextBox txtOpsCounter;
  Button btnOpGroup;

  public Widget uiEditor1() {

    if (vpEditor1 != null)
      return vpEditor1;

    vpEditor1 = new VerticalPanel();
    vpEditor1.add(editor1.getWidget());
    vpEditor1.setWidth("100%");
    setCSS(editor1.getWidget(),
        "padding", "5px",
        "margin", "10px",
        "border", "1px solid darkgray",
        "min-height", "300px");


    ctrlPanel1 = new HorizontalPanel();

    btnOpGroup = new Button("Begin Ops. group", new ClickHandler() {
      public void onClick(ClickEvent e) {

        toggleOpGrouping();

      }
    });
    ctrlPanel1.add(btnOpGroup);

    lblOpsCounter = new Label("Generated Ops #");
    ctrlPanel1.add(lblOpsCounter);
    txtOpsCounter = new TextBox();
    txtOpsCounter.setWidth("5em");
    txtOpsCounter.setValue("0");
    ctrlPanel1.add(txtOpsCounter);


    vpEditor1.add(ctrlPanel1);


    return vpEditor1;

  }


  VerticalPanel vpEditor2;
  HorizontalPanel ctrlPanel2;
  Button btnReplayDoc1Ops, btnFastForwardOps, btnRewindOps;

  public Widget uiEditor2() {

    if (vpEditor2 != null) {
      return vpEditor2;
    }

    vpEditor2 = new VerticalPanel();
    vpEditor2.add(editor2.getWidget());
    vpEditor2.setWidth("100%");
    setCSS(editor2.getWidget(),
        "padding", "5px",
        "margin", "10px",
        "border", "1px solid darkgray",
        "min-height", "300px");

    ctrlPanel2 = new HorizontalPanel();

    btnReplayDoc1Ops = new Button("Replay", new ClickHandler() {
      public void onClick(ClickEvent e) {

        replay();

      }
    });
    ctrlPanel2.add(btnReplayDoc1Ops);


    btnRewindOps = new Button(" &lt; prev op ", new ClickHandler() {
      public void onClick(ClickEvent e) {

        if (doc2 != null)
          doc2.prevDelta();

      }
    });
    ctrlPanel2.add(btnRewindOps);


    btnFastForwardOps = new Button(" next op &gt; ", new ClickHandler() {
      public void onClick(ClickEvent e) {

        if (doc2 != null)
          doc2.nextDelta();

      }
    });
    ctrlPanel2.add(btnFastForwardOps);


    vpEditor2.add(ctrlPanel2);

    return vpEditor2;

  }

  public void updateInfo1() {
    txtOpsCounter.setValue("" + opCounter);
  }

}
