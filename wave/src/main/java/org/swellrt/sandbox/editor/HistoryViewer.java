package org.swellrt.sandbox.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocRevision;
import org.waveprotocol.wave.client.editor.playback.PlaybackDocument;
import org.waveprotocol.wave.client.wave.DocOpContext;
import org.waveprotocol.wave.client.wave.DocOpTracker;
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
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A sandbox for document's history management features.
 *
 * @author pablojan@gmail.com
 *
 */
public class HistoryViewer extends Composite {

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


  //
  // DOCUMENT STUFF
  //

  DocHistory docHistory;
  PlaybackDocument playbackDoc;
  Editor docViewer;


  /** A doc op cache to query op metadata from ops consumers */
  DocOpTracker docOpCache = new DocOpTracker() {

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
      // Document viewer (using editor)
      //

      docViewer = createEditor("docViewer");
      docViewer.init(getRegistries(), new KeyBindingRegistry(), getSettings());

      docHistory = SampleDocHistories.getHistoryOne();
      playbackDoc = new PlaybackDocument(getRegistries(), getSchema(), docHistory, docOpCache);
      docViewer.setContent(playbackDoc.getDocument());
      docViewer.setEditing(false);

      // Render a content document in a panel
      /*
       * editorPanel.add(displayDoc2); doc2 =
       * createContentDocument("This is doc two", false); doc2.setRendering();
       * displayDoc2.getElement()
       * .appendChild(doc2.getFullContentView().getDocumentElement().
       * getImplNodelet());
       */




      buildUI();



    } catch (RuntimeException ex) {
      GWT.log("Fatal Exception", ex);
    }
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





  //
  // UI
  //
  DocRevision baseRevision;
  DocRevision targetRevision;

  VerticalPanel panelLeft;
  VerticalPanel panelRight;
  VerticalPanel panelPlayControl;

  Label labelShowingRevision;
  Label labelBaseRevision;
  TextBox boxTargetRevision;

  Button btnShow;
  Button btnShowDiff;

  public void buildUI() {

    //
    // Panel Left (document viewer and playback controls)
    //

    panelLeft = new VerticalPanel();
    panelLeft.add(docViewer.getWidget());
    panelLeft.setWidth("100%");
    setCSS(docViewer.getWidget(),
        "padding", "5px",
        "margin", "10px",
        "border", "1px solid darkgray",
        "min-height", "300px");


    panelPlayControl = new VerticalPanel();
    panelLeft.add(panelPlayControl);

    labelShowingRevision = new Label("Base revision #");
    panelPlayControl.add(labelShowingRevision);

    labelBaseRevision = new Label("?");
    panelPlayControl.add(labelBaseRevision);

    panelPlayControl.add(new Label("Diff with revision #"));
    boxTargetRevision = new TextBox();
    boxTargetRevision.setWidth("5em");
    boxTargetRevision.setValue("?");
    panelPlayControl.add(boxTargetRevision);


    btnShowDiff = new Button("Show diffs", new ClickHandler() {
      public void onClick(ClickEvent e) {

        try {
          int targetRevisionIndex = Integer.valueOf(boxTargetRevision.getValue());
          targetRevision = docHistory.getUnsafe(targetRevisionIndex);

          docViewer.removeContentAndUnrender();
          playbackDoc.renderDiff(baseRevision, targetRevision);
          docViewer.setContent(playbackDoc.getDocument());

        } catch (NumberFormatException ex) {

        }
      }
    });
    panelPlayControl.add(btnShowDiff);
    panelLeft.add(panelPlayControl);


    //
    // Right panel (history log)
    //

    panelRight = new VerticalPanel();

    DocHistory.Iterator history = docHistory.getIterator();

    while (history.hasPrev()) {
      history.prev(revision -> {
        panelRight.add(new Button(revision.toString(), new ClickHandler() {

          @Override
          public void onClick(ClickEvent event) {
            docViewer.removeContentAndUnrender();
            playbackDoc.render(revision);
            docViewer.setContent(playbackDoc.getDocument());
            baseRevision = revision;
            labelBaseRevision.setText("" + baseRevision.getRevisionIndex());
          }
        }));
      });
    }

    //
    //
    //

    HorizontalPanel mainPanel = new HorizontalPanel();
    mainPanel.setWidth("100%");

    mainPanel.add(panelLeft);
    mainPanel.setCellWidth(panelLeft, "75%");

    mainPanel.add(panelRight);
    mainPanel.setCellWidth(panelRight, "25%");

    initWidget(mainPanel);

  }


  public Editor getViewerEditor() {
    return docViewer;
  }



}
