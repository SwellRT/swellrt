/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.webclient.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.UIObject;

import org.waveprotocol.box.webclient.client.events.Log;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEvent;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEventHandler;
import org.waveprotocol.box.webclient.client.events.WaveCreationEvent;
import org.waveprotocol.box.webclient.client.events.WaveCreationEventHandler;
import org.waveprotocol.box.webclient.client.events.WaveSelectionEvent;
import org.waveprotocol.box.webclient.client.events.WaveSelectionEventHandler;
import org.waveprotocol.box.webclient.profile.RemoteProfileManagerImpl;
import org.waveprotocol.box.webclient.search.RemoteSearchService;
import org.waveprotocol.box.webclient.search.Search;
import org.waveprotocol.box.webclient.search.SearchPanelRenderer;
import org.waveprotocol.box.webclient.search.SearchPanelWidget;
import org.waveprotocol.box.webclient.search.SearchPresenter;
import org.waveprotocol.box.webclient.search.SimpleSearch;
import org.waveprotocol.box.webclient.search.WaveContext;
import org.waveprotocol.box.webclient.search.WaveStore;
import org.waveprotocol.box.webclient.widget.error.ErrorIndicatorPresenter;
import org.waveprotocol.box.webclient.widget.frame.FramedPanel;
import org.waveprotocol.box.webclient.widget.loading.LoadingIndicator;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.common.util.AsyncHolder.Accessor;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.client.widget.popup.CenterPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.GwtWaverefEncoder;

import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebClient implements EntryPoint {
  interface Binder extends UiBinder<DockLayoutPanel, WebClient> {
  }

  interface Style extends CssResource {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  static Log LOG = Log.get(WebClient.class);
  // Use of GWT logging is only intended for sending exception reports to the
  // server, nothing else in the client should use java.util.logging.
  // Please also see WebClientDemo.gwt.xml.
  private static final Logger REMOTE_LOG = Logger.getLogger("REMOTE_LOG");

  /** Creates a popup that warns about network disconnects. */
  private static UniversalPopup createTurbulencePopup() {
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    UniversalPopup popup =
        PopupFactory.createPopup(null, new CenterPopupPositioner(), chrome, true);
    popup.add(new HTML("<div style='color: red; padding: 5px; text-align: center;'>"
        + "<b>A turbulence detected!<br></br>"
        + " Please save your last changes to somewhere and reload the wave.</b></div>"));
    return popup;
  }

  private final ProfileManager profiles = new RemoteProfileManagerImpl();
  private final UniversalPopup turbulencePopup = createTurbulencePopup();

  @UiField
  SplitLayoutPanel splitPanel;

  @UiField
  Style style;

  @UiField
  FramedPanel waveFrame;

  @UiField
  ImplPanel waveHolder;
  private final Element loading = new LoadingIndicator().getElement();

  @UiField(provided = true)
  final SearchPanelWidget searchPanel = new SearchPanelWidget(new SearchPanelRenderer(profiles));

  @UiField
  DebugMessagePanel logPanel;

  /** The wave panel, if a wave is open. */
  private StagesProvider wave;

  private final WaveStore waveStore = new SimpleWaveStore();

  /**
   * Create a remote websocket to talk to the server-side FedOne service.
   */
  private WaveWebSocketClient websocket;

  private ParticipantId loggedInUser;

  private IdGenerator idGenerator;

  private RemoteViewServiceMultiplexer channel;

  /**
   * This is the entry point method.
   */
  @Override
  public void onModuleLoad() {

    ErrorHandler.install();

    ClientEvents.get().addWaveCreationEventHandler(
        new WaveCreationEventHandler() {

          @Override
          public void onCreateRequest(WaveCreationEvent event, Set<ParticipantId> participantSet) {
            LOG.info("WaveCreationEvent received");
            if (channel == null) {
              throw new RuntimeException("Spaghetti attack.  Create occured before login");
            }
            openWave(WaveRef.of(idGenerator.newWaveId()), true, participantSet);
          }
        });

    setupConnectionIndicator();

    HistorySupport.init(new HistoryProviderDefault());
    HistoryChangeListener.init();

    websocket = new WaveWebSocketClient(websocketNotAvailable(), getWebSocketBaseUrl());
    websocket.connect();

    if (Session.get().isLoggedIn()) {
      loggedInUser = new ParticipantId(Session.get().getAddress());
      idGenerator = ClientIdGenerator.create();
      loginToServer();
    }

    setupUi();

    History.fireCurrentHistoryState();
    LOG.info("SimpleWebClient.onModuleLoad() done");
  }

  private void setupUi() {
    // Set up UI
    DockLayoutPanel self = BINDER.createAndBindUi(this);
    RootPanel.get("app").add(self);
    // DockLayoutPanel forcibly conflicts with sensible layout control, and
    // sticks inline styles on elements without permission. They must be
    // cleared.
    self.getElement().getStyle().clearPosition();
    splitPanel.setWidgetMinSize(searchPanel, 300);

    if (LogLevel.showDebug()) {
      logPanel.enable();
    } else {
      logPanel.removeFromParent();
    }

    setupSearchPanel();
    setupWavePanel();
  }

  private void setupSearchPanel() {
    // On wave action fire an event.
    SearchPresenter.WaveActionHandler actionHandler =
        new SearchPresenter.WaveActionHandler() {
          @Override
          public void onCreateWave() {
            ClientEvents.get().fireEvent(new WaveCreationEvent());
          }

          @Override
          public void onWaveSelected(WaveId id) {
            ClientEvents.get().fireEvent(new WaveSelectionEvent(WaveRef.of(id)));
          }
        };
    Search search = SimpleSearch.create(RemoteSearchService.create(), waveStore);
    SearchPresenter.create(search, searchPanel, actionHandler, profiles);
  }

  private void setupWavePanel() {
    // Hide the frame until waves start getting opened.
    UIObject.setVisible(waveFrame.getElement(), false);

    // Handles opening waves.
    ClientEvents.get().addWaveSelectionEventHandler(new WaveSelectionEventHandler() {
      @Override
      public void onSelection(WaveRef waveRef) {
        openWave(waveRef, false, null);
      }
    });
  }

  private void setupConnectionIndicator() {
    ClientEvents.get().addNetworkStatusEventHandler(new NetworkStatusEventHandler() {

      boolean isTurbulenceDetected = false;

      @Override
      public void onNetworkStatus(NetworkStatusEvent event) {
        Element element = Document.get().getElementById("netstatus");
        if (element != null) {
          switch (event.getStatus()) {
            case CONNECTED:
            case RECONNECTED:
              element.setInnerText("Online");
              element.setClassName("online");
              isTurbulenceDetected = false;
              turbulencePopup.hide();
              break;
            case DISCONNECTED:
              element.setInnerText("Offline");
              element.setClassName("offline");
              if (!isTurbulenceDetected) {
                isTurbulenceDetected = true;
                turbulencePopup.show();
              }
              break;
            case RECONNECTING:
              element.setInnerText("Connecting...");
              element.setClassName("connecting");
              break;
          }
        }
      }
    });
  }

  /**
   * Returns <code>ws(s)://yourhost[:port]/</code>.
   */
  // XXX check formatting wrt GPE
  private native String getWebSocketBaseUrl() /*-{return ((window.location.protocol == "https:") ? "wss" : "ws") + "://" +  $wnd.__websocket_address + "/";}-*/;

  private native boolean websocketNotAvailable() /*-{ return !window.WebSocket }-*/;

  /**
   */
  private void loginToServer() {
    assert loggedInUser != null;
    channel = new RemoteViewServiceMultiplexer(websocket, loggedInUser.getAddress());
  }

  /**
   * Shows a wave in a wave panel.
   *
   * @param waveRef wave id to open
   * @param isNewWave whether the wave is being created by this client session.
   * @param participants the participants to add to the newly created wave.
   *        {@code null} if only the creator should be added
   */
  private void openWave(WaveRef waveRef, boolean isNewWave, Set<ParticipantId> participants) {
    LOG.info("WebClient.openWave()");

    if (wave != null) {
      wave.destroy();
      wave = null;
    }

    // Release the display:none.
    UIObject.setVisible(waveFrame.getElement(), true);
    waveHolder.getElement().appendChild(loading);
    Element holder = waveHolder.getElement().appendChild(Document.get().createDivElement());
    Element unsavedIndicator = Document.get().getElementById("unsavedStateContainer");
    StagesProvider wave =
        new StagesProvider(holder, unsavedIndicator, waveHolder, waveFrame, waveRef, channel, idGenerator,
            profiles, waveStore, isNewWave, Session.get().getDomain(), participants);
    this.wave = wave;
    wave.load(new Command() {
      @Override
      public void execute() {
        loading.removeFromParent();
      }
    });
    String encodedToken = History.getToken();
    if (encodedToken != null && !encodedToken.isEmpty()) {
      WaveRef fromWaveRef;
      try {
        fromWaveRef = GwtWaverefEncoder.decodeWaveRefFromPath(encodedToken);
      } catch (InvalidWaveRefException e) {
        LOG.info("History token contains invalid path: " + encodedToken);
        return;
      }
      if (fromWaveRef.getWaveId().equals(waveRef.getWaveId())) {
        // History change was caused by clicking on a link, it's already
        // updated by browser.
        return;
      }
    }
    History.newItem(GwtWaverefEncoder.encodeToUriPathSegment(waveRef), false);
  }

  /**
   * An exception handler that reports exceptions using a <em>shiny banner</em>
   * (an alert placed on the top of the screen). Once the stack trace is
   * prepared, it is revealed in the banner via a link.
   */
  static class ErrorHandler implements UncaughtExceptionHandler {
    /** Next handler in the handler chain. */
    private final UncaughtExceptionHandler next;

    /**
     * Indicates whether an error has already been reported (at most one error
     * is ever reported by this handler).
     */
    private boolean hasFired;

    private ErrorHandler(UncaughtExceptionHandler next) {
      this.next = next;
    }

    public static void install() {
      GWT.setUncaughtExceptionHandler(new ErrorHandler(GWT.getUncaughtExceptionHandler()));
    }

    @Override
    public void onUncaughtException(Throwable e) {
      if (!hasFired) {
        hasFired = true;
        final ErrorIndicatorPresenter error =
            ErrorIndicatorPresenter.create(RootPanel.get("banner"));
        getStackTraceAsync(e, new Accessor<SafeHtml>() {
          @Override
          public void use(SafeHtml stack) {
            error.addDetail(stack, null);
            REMOTE_LOG.severe(stack.asString().replace("<br>", "\n"));
          }
        });
      }

      if (next != null) {
        next.onUncaughtException(e);
      }
    }

    private void getStackTraceAsync(final Throwable t, final Accessor<SafeHtml> whenReady) {
      // TODO: Request stack-trace de-obfuscation. For now, just use the
      // javascript stack trace.
      //
      // Use minimal services here, in order to avoid the chance that reporting
      // the error produces more errors. In particular, do not use WIAB's
      // scheduler to run this command.
      // Also, this code could potentially be put behind a runAsync boundary, to
      // save whatever dependencies it uses from the initial download.
      new Timer() {
        @Override
        public void run() {
          SafeHtmlBuilder stack = new SafeHtmlBuilder();

          Throwable error = t;
          while (error != null) {
            String token = String.valueOf((new Date()).getTime());
            stack.appendHtmlConstant("Token:  " + token + "<br> ");
            stack.appendEscaped(String.valueOf(error.getMessage())).appendHtmlConstant("<br>");
            for (StackTraceElement elt : error.getStackTrace()) {
              stack.appendHtmlConstant("  ")
                  .appendEscaped(maybe(elt.getClassName(), "??")).appendHtmlConstant(".") //
                  .appendEscaped(maybe(elt.getMethodName(), "??")).appendHtmlConstant(" (") //
                  .appendEscaped(maybe(elt.getFileName(), "??")).appendHtmlConstant(":") //
                  .appendEscaped(maybe(elt.getLineNumber(), "??")).appendHtmlConstant(")") //
                  .appendHtmlConstant("<br>");
            }
            error = error.getCause();
            if (error != null) {
              stack.appendHtmlConstant("Caused by: ");
            }
          }

          whenReady.use(stack.toSafeHtml());
        }
      }.schedule(1);
    }

    private static String maybe(String value, String otherwise) {
      return value != null ? value : otherwise;
    }

    private static String maybe(int value, String otherwise) {
      return value != -1 ? String.valueOf(value) : otherwise;
    }
  }
}
