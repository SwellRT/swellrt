package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpCollector;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.version.HashedVersion;


/**
 * A fake implementation of a DocHistory that fetch and build document revisions
 * from a list of fake {@link Delta} instances. A Delta has similar structure as
 * web service result.
 * <p>
 * <br>
 * Use this class to test {@link DocHistory} and {@link PlaybackDocument}
 * offline.
 *
 */
public class DocHistoryFake extends DocHistory {

  public static class Delta {
    long timestap;
    HashedVersion appliedToVersion;
    HashedVersion resultingVersion;
    DocOp[] ops;
    String participant;
  }

  List<Delta> deltas;

  public DocHistoryFake(List<Delta> deltas) {
    super(deltas.get(deltas.size() - 1).resultingVersion);
    this.deltas = deltas;
  }

  @Override
  protected void fetchSnaphost(DocRevision revision, RawSnapshotResult callback) {
    DocOpCollector opCollector = new DocOpCollector();
    deltas.forEach(delta -> {
      if (delta.resultingVersion.getVersion() <= revision.resultingVersion.getVersion()) {
        Arrays.asList(delta.ops).forEach(op -> {
          DocRevision rev = revision;
          String debug = DocOpUtil.toConciseString(op);
          opCollector.add(op);
        });
      }
    });

    String xmlSnapshot = DocOpUtil
        .toXmlString(DocOpUtil.asInitialization(opCollector.composeAll()));
    callback.result(xmlSnapshot);
  }

  @Override
  protected void fetchRevision(HashedVersion resultingVersion,
      int nextRevisionIndex,
      RevisionListResult callback) {

    int d = 0;
    int revisionDeltaStart = -1;
    int revisionDeltaEnd = -1;
    while (revisionDeltaStart == -1 && d < deltas.size()) {
      if (deltas.get(d).resultingVersion.getVersion() == resultingVersion.getVersion()) {
        revisionDeltaStart = d;
      } else {
        d++;
      }
    }

    if (revisionDeltaStart == -1) {
      // version not found
      callback.result(new ArrayList<DocRevision>());
      return;
    }

    List<DocOp> ops = new ArrayList<DocOp>();

    // a revision is a contiguous list of deltas from same author
    // deltas is has ascending sort, now we collect deltas backwards
    String participant = deltas.get(d).participant;
    while (revisionDeltaEnd == -1 && d >= 0) {
      Delta delta = deltas.get(d);
      if (!delta.participant.equals(participant)) {
        revisionDeltaEnd = d + 1;
      } else {
        // prepend!
        ops.addAll(0, Arrays.asList(deltas.get(d).ops));
        d--;
      }
    }

    if (revisionDeltaEnd == -1)
      revisionDeltaEnd = d + 1;

    DocRevision revision = new DocRevision(this, deltas.get(revisionDeltaEnd).appliedToVersion,
        deltas.get(revisionDeltaStart).resultingVersion, deltas.get(revisionDeltaStart).timestap,
        participant, nextRevisionIndex);

    DocOpCollector opCollector = new DocOpCollector();
    ops.forEach(op -> opCollector.add(op));
    revision.setDocOp(opCollector.composeAll());

    callback.result(Collections.singletonList(revision));
  }

}
