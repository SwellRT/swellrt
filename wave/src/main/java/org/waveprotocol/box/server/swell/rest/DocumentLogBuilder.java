package org.waveprotocol.box.server.swell.rest;

import java.io.IOException;

import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.gson.ProtocolDocumentOperationGsonImpl;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.VersionUpdateOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationVisitor;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gson.stream.JsonWriter;

/**
 * The helper class to generate a document history log in JSON with or without
 * doc. operations.
 */
public class DocumentLogBuilder {

  /**
   * Checks if a WaveletOperation concern a specific Document/Blip
   */
  private static class BlipOpMatcher implements WaveletOperationVisitor {

    final String blipId;

    boolean hasMatched = false;
    ParticipantId author = null;

    public BlipOpMatcher(String blipId) {
      this.blipId = blipId;
    }

    public boolean hasMatched() {
      return hasMatched;
    }

    public void reset() {
      hasMatched = false;
      author = null;
    }

    public ParticipantId getAuthor() {
      return author;
    }

    @Override
    public void visitNoOp(NoOp op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitVersionUpdateOp(VersionUpdateOp op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitAddParticipant(AddParticipant op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitRemoveParticipant(RemoveParticipant op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitWaveletBlipOperation(WaveletBlipOperation op) {
      if (op.getBlipId().equals(blipId)) {
        hasMatched = true;
        author = op.getContext().getCreator();
      }
    }

  }

  /**
   * Generates a JSON array with metadata of each document version.
   */
  private static class LogJsonBuilder {


    final BlipOpMatcher blipMatcher;
    final JsonWriter writer;
    final boolean outputOperations;

    public LogJsonBuilder(String documentId, boolean outputOperations, JsonWriter writer) {
      this.blipMatcher = new BlipOpMatcher(documentId);
      this.writer = writer;
      this.outputOperations = outputOperations;
    }

    public void begin() throws IOException {
      writer.beginArray();
    }

    public void end() throws IOException {
      writer.endArray();
    }


    private static void serializeDocOps(TransformedWaveletDelta delta, final JsonWriter jw) {
      delta.forEach(waveletOp -> {

        if (waveletOp instanceof WaveletBlipOperation) {
          BlipOperation blipOp = ((WaveletBlipOperation) waveletOp).getBlipOp();

          if (blipOp instanceof BlipContentOperation) {
            DocOp docOp = ((BlipContentOperation) blipOp).getContentOp();

            ProtocolDocumentOperationGsonImpl gsoDocOp = WaveProtocolSerializer.serialize(docOp);
            try {
              jw.jsonValue(gsoDocOp.toGson(null, null).toString());
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          }
        }
      });
    }

    /**
     * Process a delta generating a JSON object with its data if it matches the
     * criterion.
     *
     * @return true if the delta was processed.
     */
    public boolean process(TransformedWaveletDelta delta) {

      blipMatcher.reset();

      delta.forEach(op -> {
        op.acceptVisitor(blipMatcher);
      });

      if (blipMatcher.hasMatched()) {

        try {
          writer.beginObject();
          writer.name("version").value(delta.getResultingVersion().serialise());
          writer.name("author").value(blipMatcher.author.getAddress());
          writer.name("time").value(delta.getApplicationTimestamp());

          if (outputOperations) {
            writer.name("ops");
            writer.beginArray();
            serializeDocOps(delta, writer);
            writer.endArray();
          }

          writer.endObject();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }

        return true;
      }

      return false;
    }

  }

  public static void build(WaveletProvider waveletProvider, WaveletName waveletName,
      String documentId, HashedVersion versionStart, HashedVersion versionEnd, final long limit,
      JsonWriter jw, boolean returnOperations) throws WaveServerException, IOException {

    final LogJsonBuilder logBuilder = new LogJsonBuilder(documentId, returnOperations, jw);
    logBuilder.begin();
    waveletProvider.getHistory(waveletName, versionStart, versionEnd,
        new Receiver<TransformedWaveletDelta>() {

          long count = 0;

          @Override
          public boolean put(TransformedWaveletDelta delta) {

            if (logBuilder.process(delta))
              count++;

            if (limit > 0 && count >= limit)
              return false;

            return true;
          }
        });

    logBuilder.end();
  }

}
