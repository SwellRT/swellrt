package org.waveprotocol.wave.client.editor.playback;

import java.util.List;
import java.util.Optional;

import org.waveprotocol.wave.model.document.operation.DocOp;

public interface DocHistory {

  public static class Delta {

    public List<DocOp> ops;
    public DocOpContext context;

    public Delta(List<DocOp> ops, DocOpContext context) {
      super();
      this.ops = ops;
      this.context = context;
    }

  }

  public enum DeltaUnit {
    OP, AUTHOR, TIME
  }

  /** resets the history to the initial state */
  void reset();

  /** operations to play the document to next state */
  Optional<Delta> nextOpDelta();

  /** reverse operations to rewind document to previous state */
  Optional<Delta> prevOpDelta();

  /** operations to play the document to next state */
  List<Delta> nextDelta(DeltaUnit deltaUnit);

  /** reverse operations to rewind document to previous state */
  List<Delta> prevDelta(DeltaUnit deltaUnit);


}
