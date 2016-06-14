package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpCollector;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

/**
 *
 * A real implementation of {@link OperationSequencer}. Compose all doc ops
 * performed between calls to begin() and end() methods. Nested calls are
 * supported.
 *
 * Clients must ensure paired calls to begin() - end() to avoid failure.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class GroupOperationSequencer implements OperationSequencer<Nindo> {
  private final SuperSink sink;
  private final SilentOperationSink<? super DocOp> outputSink;


  private DocOpCollector opCollector = new DocOpCollector();
  private int groupLevel;

  // private static native void log(String s) /*-{
  // console.log(s);
  // }-*/;

  public GroupOperationSequencer(SuperSink sink, SilentOperationSink<? super DocOp> outputSink) {
    this.sink = sink;
    this.outputSink = outputSink;

    this.groupLevel = 0;
  }

  @Override
  public void begin() {
    this.groupLevel++;
    // log("Operation Sequencer, begin group " + groupLevel);
  }

  @Override
  public void end() {
    // log("Operation Sequencer, end group " + groupLevel);
    this.groupLevel--;

    if (groupLevel == 0) {
      DocOp op = opCollector.composeAll();
      if (op != null) flush(op);
    }

  }

  protected void flush(DocOp docOp) {

    // log("Operation Sequencer, flush op :" + docOp.toString());
    if (outputSink != null) {
      outputSink.consume(docOp);
    }

  }

  /**
   * Applies the operation to this document, and then sends it to the output
   * sink.
   *
   * @param op mutation to apply
   */
  @Override
  public void consume(Nindo op) {

    DocOp docOp = null;

    try {
      docOp = sink.consumeAndReturnInvertible(op);
    } catch (OperationException e) {
      throw new OperationRuntimeException(
          "DocumentOperationSink constructed by DocumentOperationSinkFactory "
              + "generated an OperationException when attempting to apply " + op, e);
    }

    // log("Operation Sequencer, add op " + docOp.toString());
    opCollector.add(docOp);

  }
}