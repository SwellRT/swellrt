package org.swellrt.beta.client.platform.web.editor.history;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

import org.swellrt.beta.client.ServiceDeps;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.GetDocLogOperation;
import org.swellrt.beta.client.rest.operations.GetDocLogOperation.Response;
import org.swellrt.beta.client.rest.operations.GetDocSnapshotOperation;
import org.swellrt.beta.client.rest.operations.params.DocLogDelta;
import org.swellrt.beta.client.rest.operations.params.ResponseWrapper;
import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.ClientPercentEncoderDecoder;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocRevision;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpCollector;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;

public class DocHistoryRemote extends DocHistory {

  private HashedVersionFactory hashFactory = new HashedVersionFactoryImpl(
      new IdURIEncoderDecoder(new ClientPercentEncoderDecoder()));

  private static HashedVersion hashedVersionOf(DocLogDelta delta) {
    HashedVersion v;
    try {
      v = HashedVersion.valueOf(delta.getVersion());
    } catch (NumberFormatException e) {
      throw new IllegalStateException(e);
    } catch (Base64DecoderException e) {
      throw new IllegalStateException(e);
    }

    return v;
  }

  /** max number of deltas to fetch from remote server on every request */
  private static final int DELTAS_TO_FETCH = 200;

  final WaveId waveId;
  final WaveletId waveletId;
  final String documentId;


  public DocHistoryRemote(WaveId waveId, WaveletId waveletId, String documentId,
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

  /** Queue to process DocOp during recursive revision building */
  private final Deque<DocOp> revisionOpStack = new ArrayDeque<DocOp>();

  private DocOp composeOpStack() {
    DocOpCollector collector = new DocOpCollector();
    for (DocOp op = revisionOpStack.pollFirst(); op != null; op = revisionOpStack.pollFirst()) {
      collector.add(op);
    }
    revisionOpStack.clear();
    return collector.composeAll();
  }

  /**
   * A recursive method to build up a {@link DocRevision} calling to the remote
   * server multiple times if necessary.
   * <p>
   * <br>
   *
   *
   * @param revisionIndex the index in the history for the new revision to get
   * @param resultingVersion the version from where start building the revision
   * @param revision the DocRevision that is being built
   * @param finalCallback callback to return the revision when it is built.
   */
  private void buildRevisionRecursive(int revisionIndex, HashedVersion resultingVersion, DocRevision revision,
      RevisionResult finalCallback) {

    DocHistory history = this;

    GetDocLogOperation.Options options = new GetDocLogOperation.Options(waveId, waveletId,
        documentId);
    options.endHashVersion = resultingVersion;
    options.numberOfResults = DELTAS_TO_FETCH;
    options.orderDesc = true;
    options.returnOps = true;

    GetDocLogOperation getDocLogOperation = new GetDocLogOperation(ServiceDeps.serviceContext,
        options, new ServiceOperation.Callback<GetDocLogOperation.Response>() {

          @Override
          public void onError(SException exception) {
            finalCallback.result(null);
          }

          @Override
          public void onSuccess(Response response) {

            if (response.getLog().length == 1 && revisionIndex > 0) {
              finalCallback.result(null);
              return;
            }

            DocRevision crevision = revision;

            boolean stopDifferentDeltaAuthor = false;
            boolean stopNoMoreDeltas = response.getLog().length < DELTAS_TO_FETCH;

            for (int i = 0; i < response.getLog().length && !stopDifferentDeltaAuthor; i++) {

              DocLogDelta delta = response.getLog()[i];

              if (crevision == null) {
                crevision = new DocRevision(history, hashedVersionOf(delta), null,
                    delta.getTime(), delta.getAuthor(), revisionIndex);
              } else {

                HashedVersion deltaHashedVersion = hashedVersionOf(delta);

                if (deltaHashedVersion.equals(crevision.getAppliedAtHashedVersion())) {
                  // On second and next calls to the remote server the first
                  // delta
                  // in the response is repeated, ignore.
                  continue;
                }

                crevision.setAppliedAtHashedVersion(deltaHashedVersion);

                // Here the condition to say whether a revision is completed
                // By now, a revision is just all contiguous deltas from same
                // author.
                // TODO consider to split revisions by time chunks
                if (!crevision.getAuthor().equals(delta.getAuthor())) {
                  stopDifferentDeltaAuthor = true;
                }

              }

              if (!stopDifferentDeltaAuthor) {
                Object[] ops = delta.getOps();
                for (int j = ops.length - 1; j >= 0; j--)
                  revisionOpStack.addFirst(WaveDeps.protocolMessageUtils.deserializeDocOp(ops[j]));
              }

            }


            if (stopDifferentDeltaAuthor || stopNoMoreDeltas) {
              crevision.setDocOp(composeOpStack());
              finalCallback.result(crevision);
            } else {

              // fetch more deltas to keep building the revision
              buildRevisionRecursive(revisionIndex, crevision.getAppliedAtHashedVersion(),
                  crevision, finalCallback);

            }

          }

        });

    ServiceDeps.remoteOperationExecutor.execute(getDocLogOperation);

  }

  @Override
  protected void fetchRevision(HashedVersion resultingVersion,
      int nextRevisionIndex, RevisionListResult callback) {

    revisionOpStack.clear();
    buildRevisionRecursive(nextRevisionIndex, resultingVersion, null, new RevisionResult() {

      @Override
      public void result(DocRevision revision) {
        if (callback != null) {
          if (revision != null)
            callback.result(Collections.singletonList(revision));
          else
            callback.result(Collections.emptyList());
        }

      }
    });


  }


}
