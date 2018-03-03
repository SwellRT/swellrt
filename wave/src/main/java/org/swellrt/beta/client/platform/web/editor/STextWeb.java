package org.swellrt.beta.client.platform.web.editor;


import org.swellrt.beta.model.SText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "TextWeb")
public interface STextWeb extends SText {

  /** label to set history of doc revisions. */
  public static final String REV_HISTORY = "rev_history";

  /** label to set history of doc tags. */
  public static final String TAG_HISTORY = "tag_history";

  @JsIgnore
  public InteractiveDocument getContentDocument();

  /** attach and render this text object to a DOM container element */
  public void attachToDOM(Element element);

  /** deattach and this text object from DOM */
  public void deattachFromDOM();

  /** gets a text object that can playback a history of text changes */
  public SPlaybackText getPlaybackTextFor(@JsOptional String historyType);
}
