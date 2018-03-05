package org.swellrt.beta.client.platform.web.editor.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.swellrt.beta.client.ServiceDeps;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.GetDocRevisionOperation;
import org.swellrt.beta.client.rest.operations.GetDocRevisionOperation.Response;
import org.swellrt.beta.client.rest.operations.GetDocSnapshotOperation;
import org.swellrt.beta.client.rest.operations.params.LogDocUtils;
import org.swellrt.beta.client.rest.operations.params.ResponseWrapper;
import org.swellrt.beta.client.wave.Log;
import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.ClientPercentEncoderDecoder;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocRevision;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;

/**
 * A document's history of its revisions. Each revision is a contiguous set of
 * changes from same author.
 */
public class RevDocHistory extends DocHistory {

  private static Log log = WaveDeps.logFactory.create(RevDocHistory.class);

  private HashedVersionFactory hashFactory = new HashedVersionFactoryImpl(
      new IdURIEncoderDecoder(new ClientPercentEncoderDecoder()));

  /** max number of deltas to fetch from remote server on every request */
  private static final int REVISIONS_TO_FETCH = 10;

  final WaveId waveId;
  final WaveletId waveletId;
  final String documentId;


  public RevDocHistory(WaveId waveId, WaveletId waveletId, String documentId,
      HashedVersion topVersion) {
    super(topVersion);
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.documentId = documentId;
  }

  @Override
  protected void fetchSnaphost(DocRevision revision, RawSnapshotResult callback) {

    GetDocSnapshotOperation.Options options = new GetDocSnapshotOperation.Options(waveId, waveletId,
        documentId);
    if (revision != null)
      options.version = revision.getResultingHashedVersion();

    GetDocSnapshotOperation getDocSnapshotOperation = new GetDocSnapshotOperation(ServiceDeps.serviceContext, options, new ServiceOperation.Callback<ResponseWrapper>() {

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


  @Override
  protected void fetchRevision(HashedVersion resultingVersion, int nextRevisionIndex,
      RevisionListResult callback) {

    DocHistory history = this;

    GetDocRevisionOperation.Options options = new GetDocRevisionOperation.Options(waveId, waveletId,
        documentId, resultingVersion, REVISIONS_TO_FETCH);

    GetDocRevisionOperation getDocRevisionOp = new GetDocRevisionOperation(
        ServiceDeps.serviceContext,
        options, new ServiceOperation.Callback<GetDocRevisionOperation.Response>() {

          int nextIndex = nextRevisionIndex;

          @Override
          public void onError(SException exception) {
            log.severe("Exception fetching document revision", exception);
            if (callback != null)
              callback.result(Collections.emptyList());
          }

          @Override
          public void onSuccess(Response response) {

            if (callback == null)
              return;

            List<DocRevision> revisions = new ArrayList<DocRevision>();
            for (int i=0; i<response.getLog().length; i++) {
              revisions.add(LogDocUtils.adapt(response.getLog()[i], history, nextIndex++));
            }

            callback.result(revisions);

          }

    });

    ServiceDeps.remoteOperationExecutor.execute(getDocRevisionOp);
  }



}
