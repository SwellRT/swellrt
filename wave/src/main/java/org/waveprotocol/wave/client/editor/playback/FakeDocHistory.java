package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class FakeDocHistory implements DocHistory {


  private final List<Delta> history = new ArrayList<Delta>();

  private long accVersion = 0;
  private int indexAtDeltas = 0;

  public FakeDocHistory() {
  }

  public void addAsDelta(ParticipantId creator, List<DocOp> ops) {

    Preconditions.checkArgument(ops != null && !ops.isEmpty(), "Empty or null ops list");
    Preconditions.checkArgument(creator != null, "Null participant id as creator");

    accVersion += ops.size();

    DocOpContext ctx = new DocOpContext(System.currentTimeMillis(), creator, ops.size(),
        HashedVersion.unsigned(accVersion));

    history.add(new Delta(new ArrayList<DocOp>(ops), ctx));

  }

  @Override
  public void reset() {
    accVersion = 0;
    indexAtDeltas = 0;

  }

  @Override
  public Optional<Delta> nextOpDelta() {

    if (indexAtDeltas < history.size())
      return Optional.of(history.get(indexAtDeltas++));
    else
      return Optional.empty();
  }



  @Override
  public Optional<Delta> prevOpDelta() {

    if (indexAtDeltas > 0) {
      Delta delta = history.get(--indexAtDeltas);
      return Optional.of(delta);
    } else
      return Optional.empty();
  }

  @Override
  public List<Delta> nextDelta(DeltaUnit deltaUnit) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Delta> prevDelta(DeltaUnit deltaUnit) {
    // TODO Auto-generated method stub
    return null;
  }




}
