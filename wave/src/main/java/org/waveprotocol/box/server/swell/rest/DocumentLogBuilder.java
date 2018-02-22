package org.waveprotocol.box.server.swell.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.gson.ProtocolDocumentOperationGsonImpl;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpCollector;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.VersionUpdateOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
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
  private static class OpMatcher implements WaveletOperationVisitor {

    final String blipId;

    boolean hasMatched = false;
    ParticipantId author = null;

    public OpMatcher(String blipId) {
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

    /** @return true iff the op applies to the blip */
    public boolean match(WaveletBlipOperation op) {

      this.reset();
      visitWaveletBlipOperation(op);
      return hasMatched;

    }

    /**
     * @return true iff any of the operations in the delta matches.
     */
    public boolean match(TransformedWaveletDelta delta) {

      this.reset();
      delta.forEach(op -> {
        op.acceptVisitor(this);
      });
      return hasMatched;

    }

  }

  /**
   * Generates a JSON array with metadata of each document version.
   */
  private static class LogJsonBuilder {

    final JsonWriter writer;
    final boolean outputOperations;

    public LogJsonBuilder(String documentId, boolean outputOperations, JsonWriter writer) {
      this.writer = writer;
      this.outputOperations = outputOperations;
    }

    public void begin() throws IOException {
      writer.beginArray();
    }

    public void end() throws IOException {
      writer.endArray();
    }

    private static void serializeDocOps(List<WaveletOperation> delta, final JsonWriter jw) {
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

    private static void composeAndSerializeDocOps(List<WaveletOperation> ops, final JsonWriter jw) {

      DocOpCollector opCollector = new DocOpCollector();

      ops.forEach(waveletOp -> {

        if (waveletOp instanceof WaveletBlipOperation) {
          BlipOperation blipOp = ((WaveletBlipOperation) waveletOp).getBlipOp();

          if (blipOp instanceof BlipContentOperation) {
            DocOp docOp = ((BlipContentOperation) blipOp).getContentOp();
            opCollector.add(docOp);
          }
        }
      });

      DocOp composedDocOp = opCollector.composeAll();

      ProtocolDocumentOperationGsonImpl gsoDocOp = WaveProtocolSerializer.serialize(composedDocOp);
      try {
        jw.jsonValue(gsoDocOp.toGson(null, null).toString());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }

    }

    /**
     * Process a delta generating the JSON object with its data.
     */
    public void process(TransformedWaveletDelta delta) {

      try {
        writer.beginObject();
        writer.name("version").value(delta.getResultingVersion().serialise());
        writer.name("author").value(delta.getAuthor().getAddress());
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

    }

    public void proccess(HashedVersion appliedAtVersion, HashedVersion resultingVersion,
        long applicationTime, ParticipantId author, List<WaveletOperation> ops) {
      try {
        writer.beginObject();

        writer.name("appliedAt").value(appliedAtVersion.serialise());
        writer.name("resulting").value(resultingVersion.serialise());
        writer.name("author").value(author.getAddress());
        writer.name("time").value(applicationTime);

        if (outputOperations) {
          writer.name("op");
          composeAndSerializeDocOps(ops, writer);
        }

        writer.endObject();
      } catch (IOException e) {
        throw new IllegalStateException(e);

      }

    }

  }


  public static void queryGroupBy(JsonWriter jw, WaveletProvider waveletProvider,
      WaveletName waveletName, String documentId, HashedVersion versionStart,
      HashedVersion versionEnd, boolean returnOperations, boolean sliceByUsers, long sliceDuration,
      int slicesCount) throws WaveServerException, IOException {

    OpMatcher blipMatcher = new OpMatcher(documentId);
    boolean descOrder = versionStart.getVersion() > versionEnd.getVersion();

    final LogJsonBuilder logBuilder = new LogJsonBuilder(documentId, returnOperations, jw);
    logBuilder.begin();

    waveletProvider.getHistory(waveletName, versionStart, versionEnd,
        new Receiver<TransformedWaveletDelta>() {

          List<WaveletOperation> ops = new ArrayList<WaveletOperation>();
          long sliceStartTime = 0;
          int sliceNum = 0;

          ParticipantId sliceParticipant = null;

          HashedVersion resultingVersion = null;
          long resultingTime = 0;
          HashedVersion appliedAtVersion = null;
          ParticipantId participant = null;

          @Override
          public boolean put(TransformedWaveletDelta delta) {

            if (!blipMatcher.match(delta))
              return true;

            try {

              if (descOrder) {
                appliedAtVersion = delta.getResultingVersion();
                resultingTime = delta.getApplicationTimestamp();
              } else {
                resultingVersion = delta.getResultingVersion();
                participant = delta.getAuthor();
              }

              boolean startSlice = false;

              // New slice by author change ?
              if (sliceByUsers
                  && (sliceParticipant == null || !sliceParticipant.equals(delta.getAuthor()))) {
                sliceParticipant = delta.getAuthor();
                startSlice = true;
              }

              // New slice by duration slot ?
              if ((sliceDuration > 0) && (sliceStartTime == 0
                  || (delta.getApplicationTimestamp() - sliceStartTime) > sliceDuration)) {
                sliceStartTime = delta.getApplicationTimestamp();
                startSlice = true;
              }


              if (startSlice) {

                startSlice = false;

                if (sliceNum > 0) {

                  logBuilder.proccess(appliedAtVersion, resultingVersion, resultingTime,
                      participant, ops);
                  ops.clear();
                }

                if (sliceNum + 1 > slicesCount) {
                  return false;
                }

                // start new slice
                sliceNum++;

                resultingTime = 0;
                appliedAtVersion = null;
                resultingVersion = null;
                participant = null;

                if (descOrder) {
                  resultingVersion = delta.getResultingVersion();
                  participant = delta.getAuthor();
                } else {
                  // In asc. order, this version is unsigned.
                  appliedAtVersion = HashedVersion.unsigned(delta.getAppliedAtVersion());
                  resultingTime = delta.getApplicationTimestamp();
                }

              }


              if (descOrder) {
                for (int i = delta.size() - 1; i >= 0; i--)
                  ops.add(0, delta.get(i));
              } else {
                for (int i = 0; i < delta.size(); i++)
                  ops.add(delta.get(i));
              }

            } catch (Exception e) {
              throw new IllegalStateException(e);
            }

            return true;
          }

        });


    logBuilder.end();

  }

  public static void queryAll(JsonWriter jw, WaveletProvider waveletProvider,
      WaveletName waveletName,
      String documentId, HashedVersion versionStart, HashedVersion versionEnd, final long deltasCount,
      boolean returnOperations)
      throws WaveServerException, IOException {

    OpMatcher blipMatcher = new OpMatcher(documentId);


    final LogJsonBuilder logBuilder = new LogJsonBuilder(documentId, returnOperations, jw);
    logBuilder.begin();


    waveletProvider.getHistory(waveletName, versionStart, versionEnd,
        new Receiver<TransformedWaveletDelta>() {

          long count = 0;

          @Override
          public boolean put(TransformedWaveletDelta delta) {

            if (!blipMatcher.match(delta))
              return true;

            try {

              logBuilder.process(delta);
              count++;


                if (deltasCount > 0 && count >= deltasCount)
                  return false;


            } catch (Exception e) {
              throw new IllegalStateException(e);
            }

            return true;
          }

        });


    logBuilder.end();
  }



}
