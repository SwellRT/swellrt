package org.swellrt.beta.client.platform.web.editor.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.swellrt.beta.client.ServiceDeps;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.GetDocDeltasOperation;
import org.swellrt.beta.client.rest.operations.GetDocSnapshotOperation;
import org.swellrt.beta.client.rest.operations.params.LogDocUtils;
import org.swellrt.beta.client.rest.operations.params.ResponseWrapper;
import org.swellrt.beta.client.wave.Log;
import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SVersionManager.Tag;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocRevision;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * A document's history of its version tags.
 */
public class TagDocHistory extends DocHistory {

  private static Log log = WaveDeps.logFactory.create(TagDocHistory.class);

  final WaveId waveId;
  final WaveletId waveletId;
  final String documentId;
  final Tag[] tags;

  /**
   * Creates a new instance.
   *
   * @param waveId
   * @param waveletId
   * @param documentId
   * @param topVersion
   *          the most recent version of the document (as wavelet version)
   * @param tags
   *          newest tag is last
   */
  public TagDocHistory(WaveId waveId, WaveletId waveletId, String documentId,
      HashedVersion topVersion, Tag[] tags) {
    super(topVersion);
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.documentId = documentId;
    this.tags = tags;
  }


  @Override
  protected void fetchRevision(HashedVersion resultingVersion, int nextRevisionIndex,
      RevisionListResult callback) {

    DocHistory history = this;

    GetDocDeltasOperation.Options options = new GetDocDeltasOperation.Options(waveId, waveletId,
        documentId);
    options.groupOps = true;
    options.orderDesc = true;
    options.endHashVersion = resultingVersion;

    if (nextRevisionIndex < tags.length) {
      options.startHashVersion = tags[tags.length - nextRevisionIndex - 1].version;
    }

    GetDocDeltasOperation getDocDeltasOp = new GetDocDeltasOperation(ServiceDeps.serviceContext,
        options, new ServiceOperation.Callback<GetDocDeltasOperation.Response>() {

          int nextIndex = nextRevisionIndex;

          @Override
          public void onError(SException exception) {
            log.severe("Exception fetching document revision", exception);
            if (callback != null)
              callback.result(Collections.emptyList());
          }

          @Override
          public void onSuccess(
              org.swellrt.beta.client.rest.operations.GetDocDeltasOperation.Response response) {

            if (callback == null)
              return;

            List<DocRevision> revisions = new ArrayList<DocRevision>();
            for (int i = 0; i < response.getLog().length; i++) {
              // attach tag info to the DocRevision object
              DocRevision rev = LogDocUtils.adapt(response.getLog()[i], history, nextIndex++);
              Tag tag = getTagForDocRevision(rev);
              rev.info = tag;
              revisions.add(rev);
            }

            callback.result(revisions);

          }

        });

    ServiceDeps.remoteOperationExecutor.execute(getDocDeltasOp);

  }

  private Tag getTagForDocRevision(DocRevision rev) {
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].version.getVersion() == (rev.getResultingVersion()))
        return tags[i];
    }
    return null;
  }

  @Override
  protected void fetchSnaphost(DocRevision revision, RawSnapshotResult callback) {

    GetDocSnapshotOperation.Options options = new GetDocSnapshotOperation.Options(waveId, waveletId,
        documentId);
    if (revision != null)
      options.version = revision.getResultingHashedVersion();

    GetDocSnapshotOperation getDocSnapshotOperation = new GetDocSnapshotOperation(
        ServiceDeps.serviceContext, options, new ServiceOperation.Callback<ResponseWrapper>() {

          @Override
          public void onError(SException exception) {
            if (callback != null)
              callback.result(null);
          }

          @Override
          public void onSuccess(ResponseWrapper response) {
            if (callback != null)
              callback.result(response.getResponseString());
          }

        });

    ServiceDeps.remoteOperationExecutor.execute(getDocSnapshotOperation);

  }

}
