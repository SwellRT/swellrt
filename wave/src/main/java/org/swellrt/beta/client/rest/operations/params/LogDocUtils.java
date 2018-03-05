package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.wave.WaveDeps;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocRevision;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.version.HashedVersion;

public class LogDocUtils {

  public static DocRevision adapt(LogDocRevision logDocRevision, DocHistory history, int index) {

    DocRevision docRevision = new DocRevision(history,
        hashedVersionOf(logDocRevision.getResulting()),
        hashedVersionOf(logDocRevision.getAppliedAt()), logDocRevision.getTime(),
        logDocRevision.getAuthor(), index);

    if (logDocRevision.getOp() != null)
      docRevision.setDocOp(WaveDeps.protocolMessageUtils.deserializeDocOp(logDocRevision.getOp()));

    return docRevision;
  }

  public static DocRevision adapt(LogDocDelta logDocDelta, DocHistory history, int index) {

    DocRevision docRevision = new DocRevision(history, hashedVersionOf(logDocDelta.getResulting()),
        hashedVersionOf(logDocDelta.getAppliedAt()), logDocDelta.getTime(), logDocDelta.getAuthor(),
        index);
    if (logDocDelta.getOp() != null)
      docRevision.setDocOp(WaveDeps.protocolMessageUtils.deserializeDocOp(logDocDelta.getOp()));

    return docRevision;
  }

  public static HashedVersion hashedVersionOf(String str) {
    HashedVersion v;
    try {
      v = HashedVersion.valueOf(str);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(e);
    } catch (Base64DecoderException e) {
      throw new IllegalStateException(e);
    }

    return v;
  }

}
