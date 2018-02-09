package org.swellrt.sandbox.editor;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class SandboxEntryPoint implements EntryPoint {

  public void onModuleLoad() {

    VerticalPanel basePanel = new VerticalPanel();
    basePanel.setWidth("100%");

    HTML titleHtml = new HTML("<div> Document History Viewer </div>");
    basePanel.add(titleHtml);

    HistoryViewer historyViewer = new HistoryViewer();
    historyViewer.init();

    basePanel.add(historyViewer);
    RootPanel.get().add(basePanel);

    historyViewer.getViewerEditor().focus(true);
  }


}
