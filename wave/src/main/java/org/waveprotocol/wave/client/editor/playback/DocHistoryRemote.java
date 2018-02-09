package org.waveprotocol.wave.client.editor.playback;

import org.waveprotocol.wave.model.version.HashedVersion;

public class DocHistoryRemote extends DocHistory {



  public DocHistoryRemote(HashedVersion topVersion) {
    super(topVersion);
  }

  @Override
  protected void fetchSnaphost(DocRevision revision, RawSnapshotResult callback) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void fetchOps(DocRevision revision, DocOpResult callback) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void fetchRevision(HashedVersion resultingVersion, int fetchCount,
      int nextRevisionIndex, MultipleRevisionResult callback) {
    // TODO Auto-generated method stub

  }


}
