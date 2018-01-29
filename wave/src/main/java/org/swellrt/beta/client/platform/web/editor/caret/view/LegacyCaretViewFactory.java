package org.swellrt.beta.client.platform.web.editor.caret.view;

import org.swellrt.beta.client.platform.web.editor.caret.CaretView;
import org.swellrt.beta.client.platform.web.editor.caret.CaretViewFactory;

public final class LegacyCaretViewFactory implements CaretViewFactory {

  public static final LegacyCaretViewFactory instance = new LegacyCaretViewFactory();

  @Override
  public CaretView exec() {
    return new LegacyCaretView();
  }

}
