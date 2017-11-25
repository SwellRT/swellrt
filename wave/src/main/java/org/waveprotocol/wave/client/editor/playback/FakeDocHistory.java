package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.waveprotocol.wave.client.wave.DocOpContext;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class FakeDocHistory implements DocHistory {


  private final List<Delta> history = new ArrayList<Delta>();

  private long accVersion = 0;
  private int historyIndex = 0;

  public FakeDocHistory() {
  }

  /**
   * Add a list of ops as a single delta into the history.
   *
   * @param creator
   * @param ops
   */
  public void addSingleDelta(ParticipantId creator, List<DocOp> ops) {

    Preconditions.checkArgument(ops != null && !ops.isEmpty(), "Empty or null ops list");
    Preconditions.checkArgument(creator != null, "Null participant id as creator");

    accVersion += ops.size();

    DocOpContext ctx = new DocOpContext(System.currentTimeMillis(), creator, ops.size(),
        HashedVersion.unsigned(accVersion));

    history.add(new Delta(new ArrayList<DocOp>(ops), ctx));

  }

  /**
   * Add a list of ops as a single delta into the history.
   *
   * @param creator
   * @param ops
   */
  public void addSingleDelta(ParticipantId creator, DocOp op) {

    Preconditions.checkArgument(op != null, "Empty or null ops list");
    Preconditions.checkArgument(creator != null, "Null participant id as creator");

    accVersion++;

    DocOpContext ctx = new DocOpContext(System.currentTimeMillis(), creator, 1,
        HashedVersion.unsigned(accVersion));

    history.add(new Delta(CollectionUtils.newArrayList(op), ctx));

  }

  /**
   * Add a list of ops as individuals deltas
   *
   * @param creator
   * @param ops
   */
  public void addAllDeltas(ParticipantId creator, List<DocOp> ops) {

    Preconditions.checkArgument(ops != null && !ops.isEmpty(), "Empty or null ops list");
    Preconditions.checkArgument(creator != null, "Null participant id as creator");

    ops.forEach(op -> {

      DocOpContext ctx = new DocOpContext(System.currentTimeMillis(), creator, ops.size(),
          HashedVersion.unsigned(accVersion++));

      history.add(new Delta(CollectionUtils.newArrayList(op), ctx));

    });

  }

  @Override
  public void reset() {
    accVersion = 0;
    historyIndex = 0;

  }

  @Override
  public Optional<Delta> nextDelta() {

    if (historyIndex < history.size())
      return Optional.of(history.get(historyIndex++));
    else
      return Optional.empty();
  }



  @Override
  public Optional<Delta> prevDelta() {

    if (historyIndex > 0) {
      Delta delta = history.get(--historyIndex);
      return Optional.of(delta);
    } else
      return Optional.empty();
  }

  @Override
  public List<Revision> queryRevisions(RevCriteria criteria, long startVersion,
      int numOfRevisions) {

    // Fake implementation, generates all revisions

    if (startVersion > accVersion)
      return CollectionUtils.newArrayList();

    List<Revision> revs = CollectionUtils.newArrayList();

    int revCounter = 0;

    for (int i = 0; i < history.size(); i++) {
      Delta delta = history.get(i);
      revs.add(new Revision(i, new ParticipantId[] { delta.context.getCreator() },
          delta.context.getTimestamp()));
    }


    return revs;
  }


  @Override
  public List<Delta> toDelta(long version) {
    List<Delta> deltas = CollectionUtils.newArrayList();

    if (version > historyIndex) {
      for (int i = historyIndex; i < version; i++) {
        deltas.add(history.get(i));
      }
    } else if (version < historyIndex) {
      for (int i = historyIndex; i > version; i--) {
        deltas.add(history.get(i));
      }
    }

    return deltas;
  }

  @Override
  public long getLastVersion() {
    return history.size();
  }



}
