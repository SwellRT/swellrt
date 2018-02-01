package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.client.wave.DocOpContext;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;

/**
 * The history of document operations. Operations are grouped in Deltas, which
 * has operations performed in a period of time by the same participant.
 * <p>
 * <br>
 * History allows to navigate through document versions (Deltas).
 *
 * @author pablojan@gmail.com
 *
 */
public interface DocHistory extends Iterable<DocHistory.Delta> {

  /**
   * A list of contiguous {@link DocOp}s in a document's history.
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
   * An Iterator to move through the document's history
   */
  public interface Iterator extends java.util.Iterator<Delta> {

    /** return the Delta to move back to previous state */
    Delta prev();

    /** reset the iterator, move to initial state */
    void reset();

    /**
     * seek to a particular version. returns all deltas from 0 to requested
     * version
     */
    List<Delta> seek(long version);

    /** return context of current state */
    Delta current();

  }

  public long getEndVersion();

  public DocHistory.Iterator iterator();
}
