package org.swellrt.beta.client.platform.web.editor;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

/**
 * A class to define editor's settings. Create a javascript object with
 * following properties and pass it to {@link SEditor}.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
@JsType(isNative = true, namespace = "swell.Editor", name = "Config")
public class SEditorConfig {

  public Boolean traceUserAgent;

  public Boolean consoleLog;

  public Boolean debugDialog;

  public Boolean undo;

  public Boolean fancyCursorBias;

  public Boolean semanticCopyPaste;

  public Boolean whitelistEditor;

  public Boolean webkitComposition;


  /*
   * Methods with secure default value if property is not available
   */

  @JsOverlay
  public final boolean traceUserAgent() {
    boolean DEFAULT = false;
    try {
      return traceUserAgent != null ? traceUserAgent : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final boolean consoleLog() {
    boolean DEFAULT = false;
    try {
      return consoleLog != null ? consoleLog : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final boolean debugDialog() {
    boolean DEFAULT = false;
    try {
      return debugDialog != null ? debugDialog : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final boolean undo() {
    boolean DEFAULT = true;
    try {
      return undo != null ? undo : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final boolean fancyCursorBias() {
    boolean DEFAULT = true;
    try {
      return fancyCursorBias != null ? fancyCursorBias : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final boolean semanticCopyPaste() {
    boolean DEFAULT = false;
    try {
      return semanticCopyPaste != null ? semanticCopyPaste : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final boolean whitelistEditor() {
    boolean DEFAULT = false;
    try {
      return whitelistEditor != null ? whitelistEditor : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final boolean webkitComposition() {
    boolean DEFAULT = false;
    try {
      return webkitComposition != null ? webkitComposition : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

}
