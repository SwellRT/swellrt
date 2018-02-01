package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.waveprotocol.wave.client.wave.DocOpContext;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Implementation based on local list of document operations. Use for demo
 * purposes.
 *
 * @author pablojan@gmail.com
 *
 */
public class DocHistoryLocal implements DocHistory {


  public class IteratorLocal implements DocHistory.Iterator {

    /** iterator pointer in the context's list */
    private int contextPointer = -1;

    @Override
    public boolean hasNext() {
      return contexts.size() > contextPointer + 1;
    }

    @Override
    public Delta next() {
      if (!hasNext())
        throw new IndexOutOfBoundsException();

      return getDelta(++contextPointer);
    }

    @Override
    public Delta prev() {
      if (contextPointer == 0)
        return null;

      return getDelta(--contextPointer);
    }

    private Delta getDelta(int contextIndex) {

      DocOpContext deltaCtx = contexts.get(contextIndex);
      List<DocOp> deltaOps = new ArrayList<DocOp>();

      long fromDocOp = deltaCtx.getHashedVersion().getVersion() - deltaCtx.getVersionIncrement();
      long toDocOp = deltaCtx.getHashedVersion().getVersion();

      for (long i = fromDocOp; i < toDocOp; i++) {

        if (i > Integer.MAX_VALUE)
          throw new IllegalStateException("Document history index too large");

        int ii = (int) i;
        deltaOps.add(ops.get(ii));

      }

      return new Delta(deltaOps, deltaCtx);
    }

    @Override
    public void reset() {
      this.contextPointer = -1;

    }

    @Override
    public List<Delta> seek(long version) {

      if (version < 0
          || version > contexts.get(contexts.size() - 1).getHashedVersion().getVersion())
        throw new IndexOutOfBoundsException();


      List<Delta> deltas = new ArrayList<Delta>();
      int contextIndex = -1;

      for (int i = 0; i < contexts.size(); i++) {

        deltas.add(getDelta(i));

        if (contexts.get(i).getHashedVersion().getVersion() > version) {
          contextIndex = i;
          break;
        }

      }

      if (contextIndex != -1) {
        contextPointer = contextIndex;
      }

      return deltas;
    }


    @Override
    public Delta current() {
      if (contextPointer == -1)
        return null;

      return getDelta(contextPointer);
    }

  }

  private final List<DocOp> ops = new ArrayList<DocOp>();
  private final List<DocOpContext> contexts = new ArrayList<DocOpContext>();
  private boolean isMakeNewContext = false;

  @Override
  public DocHistory.Iterator iterator() {
    return new IteratorLocal();
  }

  @Override
  public long getEndVersion() {
    if (contexts.isEmpty()) {
      return 0;
    } else {
      DocOpContext ctx = contexts.get(contexts.size() - 1);
      return ctx.getHashedVersion().getVersion();
    }

  }

  /**
   * Adds one DocOp to the history.
   *
   * @param participantId
   * @param op
   */
  public void add(ParticipantId participantId, DocOp op) {

    add(participantId, Collections.singletonList(op));
  }

  /**
   * Adds a list of DocOp's to the history.
   *
   * @param participantId
   * @param ops
   */
  public void add(ParticipantId participantId, Collection<DocOp> newOps) {

    this.ops.addAll(newOps);
    long numOps = newOps.size();


    if (contexts.isEmpty()) {

      contexts.add(new DocOpContext(System.currentTimeMillis(), participantId, numOps,
          HashedVersion.unsigned(1)));

    } else {

      int lastIndex = contexts.size() - 1;
      DocOpContext lastCtx = contexts.get(lastIndex);
      HashedVersion nextVersion = HashedVersion
          .unsigned(lastCtx.getHashedVersion().getVersion() + numOps);

      if (lastCtx.getCreator().equals(participantId) && !isMakeNewContext) {

        long versionIncrement = lastCtx.getVersionIncrement() + numOps;
        contexts.set(lastIndex, new DocOpContext(System.currentTimeMillis(), participantId,
            versionIncrement, nextVersion));

      } else {

        contexts
            .add(new DocOpContext(System.currentTimeMillis(), participantId, numOps, nextVersion));

        isMakeNewContext = false;
      }

    }

  }

  /**
   * Notify that next ops added to the history must be assigned to a new delta.
   */
  public void markDelta() {
    isMakeNewContext = true;
  }

}
