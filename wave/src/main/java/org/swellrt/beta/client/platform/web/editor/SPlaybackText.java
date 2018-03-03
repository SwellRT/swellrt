package org.swellrt.beta.client.platform.web.editor;

import org.swellrt.beta.client.platform.web.editor.history.DocHistoryRemote;
import org.swellrt.beta.model.wave.WaveSchemas;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocRevision;
import org.waveprotocol.wave.client.editor.playback.PlaybackDocument;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "TextWebPlayback")
public class SPlaybackText {

  public static SPlaybackText createForRevisionHistory(STextWeb text) {

    if (text instanceof SWaveText) {

      SWaveText wText = (SWaveText) text;
      WaveId waveId = wText.getNodeManager().getWaveId();
      WaveletId waveletId = wText.getSubstrateId().getContainerId();
      String documentId = wText.getSubstrateId().getDocumentId();

      DocHistory revHistory = new DocHistoryRemote(waveId, waveletId, documentId,
          wText.getLastVersion());

      return new SPlaybackText(new PlaybackDocument(Editor.ROOT_REGISTRIES,
          WaveSchemas.STEXT_SCHEMA_CONSTRAINTS, revHistory));

    }

    return null;

  }

  public static SPlaybackText createForTagHistory(STextWeb text) {

    if (text instanceof SWaveText) {

      SWaveText wText = (SWaveText) text;
      WaveId waveId = wText.getNodeManager().getWaveId();
      WaveletId waveletId = wText.getSubstrateId().getContainerId();
      String documentId = wText.getSubstrateId().getDocumentId();

      DocHistory tagHistory = null; // TODO

      return new SPlaybackText(null);

    }

    return null;

  }

  private final PlaybackDocument playbackDoc;
  private Element docElement;

  private Element parentElement;

  protected SPlaybackText(PlaybackDocument playbackDoc) {
    this.playbackDoc = playbackDoc;
  }

  /** attach and render this text object to a DOM container element */
  public void attachToDOM(Element element) {
    Preconditions.checkNotNull(element, "Can't attach text to empty element");
    parentElement = element;
    refreshDOM();
  }

  private void refreshDOM() {
    if (parentElement != null) {
      deattachFromDOM();
      docElement = playbackDoc.getElement();
      parentElement.appendChild(docElement);
    }
  }

  /** deattach this text object from DOM */
  public void deattachFromDOM() {
    if (docElement != null && docElement.getParentElement() != null) {
      docElement.removeFromParent();
      docElement = null;
    }
  }

  public DocHistory.Iterator getHistoryIterator() {
    return playbackDoc.getHistoryIterator();
  }

  public void render(@JsOptional DocRevision rev) {

    if (rev == null) {

        DocHistory.Iterator it = playbackDoc.getHistoryIterator();
        it.reset();
        it.prev(pRev -> {
          playbackDoc.render(pRev, doc -> {
          refreshDOM();
          });

        });


    } else {
      playbackDoc.render(rev, doc -> {
        refreshDOM();
      });
    }

  }

  public void renderDiff(DocRevision baseRev, DocRevision targetRev) {
    Preconditions.checkNotNull(baseRev, "Missing base revision");
    Preconditions.checkNotNull(targetRev, "Missing target revision");
    playbackDoc.renderDiff(baseRev, targetRev, doc -> {
      refreshDOM();
    });
  }

  public void renderNext() {
    playbackDoc.renderNext(doc -> {
    });
  }

  public void renderPrev() {
    playbackDoc.renderPrev(doc -> {
    });
  }

  public void clear() {
    deattachFromDOM();
    playbackDoc.getDocument().setShelved();
    playbackDoc.getHistoryIterator().reset();
  }

}
