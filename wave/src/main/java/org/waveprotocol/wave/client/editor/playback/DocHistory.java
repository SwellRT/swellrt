package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.waveprotocol.wave.client.wave.DocOpContext;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.wave.ParticipantId;

public interface DocHistory {


  /**
   * A delta is a set of ops from same author.
   */
  public static class Delta {

    public List<DocOp> ops;
    public DocOpContext context;

    public Delta(List<DocOp> ops, DocOpContext context) {
      super();
      this.ops = ops;
      this.context = context;
    }

    public Delta invert() {

      List<DocOp> invertedOps = new ArrayList<DocOp>(ops.size());

      for (int i = ops.size() - 1; i >= 0; i--) {
        invertedOps.add(DocOpInverter.invert(ops.get(i)));
      }

      return new Delta(invertedOps, context);
    }

  }

  /**
   * A revision is a point in the document version history meant by a group
   * criteria (author, day...)
   */
  public static class Revision {

    /** wavelet version of this revision */
    public long wversion;

    public ParticipantId[] participants;

    public long timestamp;

    public Revision(long wversion, ParticipantId[] participants, long timestamp) {
      super();
      this.wversion = wversion;
      this.participants = participants;
      this.timestamp = timestamp;
    }

  }

  public enum RevCriteria {
    OP, AUTHOR, TIME
  }

  //
  // Querying document revisions
  //

  /** returns the list of revisions for a criteria */
  List<Revision> queryRevisions(RevCriteria criteria, long startVersion, int numOfRevisions);

  long getLastVersion();

  //
  // Sequential access to deltas
  //

  /** resets the history to the initial state */
  void reset();

  /** operations to play the document to next state */
  Optional<Delta> nextDelta();

  /** operations to rewind document to previous state */
  Optional<Delta> prevDelta();

  /**
   * If requested version is lower than internal pointer, returns all ops backwards.
   * If requested version is greater than internal pointer, returns all ops
   * up to the version.
   * <p>
   * This method doesn't return inverted ops.
   *
   */
  List<Delta> toDelta(long version);

  //

}
