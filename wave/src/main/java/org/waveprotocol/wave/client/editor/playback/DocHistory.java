package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.version.HashedVersion;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * Manage the document history of revisions from a human perspective. So we
 * avoid to expose specific Wavelet/Blip concepts here. <br>
 * <p>
 * A document's revision is defined as a contiguous group of document operations
 * with same author.
 * <p>
 * <br>
 * Stored revisions are sorted by version, descending / latest revision first
 * (i.e. descending sort of delta versions). A remote service is requested to
 * fetch older revisions which are appended to the internal store.
 *
 * @author pablojan@gmail.com
 *
 */
@JsType
public abstract class DocHistory {

  @JsFunction
  @FunctionalInterface
  public interface RevisionResult {
    void result(DocRevision revision);
  }

  @JsFunction
  @FunctionalInterface
  public interface RevisionListResult {
    void result(List<DocRevision> revisionArray);
  }

  @FunctionalInterface
  public interface SnapshotResult {
    void result(DocInitialization snapshot);
  }

  @FunctionalInterface
  public interface RawSnapshotResult {
    void result(String xmlSnapshot);
  }

  @FunctionalInterface
  public interface DocOpResult {
    public void result(DocOp ops);
  }

  @FunctionalInterface
  public interface DocOpArrayResult {
    public void result(DocOp[] ops);
  }

  @JsType
  public class Iterator {

    /**
     * current revision of the iterator
     */
    private int revPointer = -1;

    public void next(RevisionResult callback) {
      if (revPointer <= 0)
        if (callback != null)
          callback.result(null);

      // we can always go back in the revision's list
      getRevision(--revPointer, callback);
    }

    public void prev(RevisionResult callback) {
      getRevision(revPointer + 1, rev -> {
        if (rev != null) revPointer++;
        if (callback != null)
          callback.result(rev);
      });
    }

    public void current(RevisionResult callback) {
      if (revPointer >= 0)
        getRevision(revPointer, callback);
      else if (callback != null)
        callback.result(null);
    }


    public void reset() {
      this.revPointer = -1;
    }

  }

  /** List of document revisions, newest version first (descending sort) */
  private final List<DocRevision> revisions = new ArrayList<DocRevision>();

  private final HashedVersion topVersion;

  @JsIgnore
  public DocHistory(HashedVersion topVersion) {
    this.topVersion = topVersion;
  }

  protected DocRevision getLastRevision() {

    return revisions.get(revisions.size() - 1);
  }

  private void getRevisionSafe(int index, RevisionResult callback) {

    if (callback == null)
      return;

    if (revisions.size() > index) {
      callback.result(revisions.get(index));
    } else {
      callback.result(null);
    }
  }

  /** Gets a revision, fetching it remotely if necessary */
  private void getRevision(int index, RevisionResult callback) {

    if (revisions.size() <= index) {

      HashedVersion versionToFetch = topVersion;
      if (!revisions.isEmpty())
        versionToFetch = getLastRevision().appliedAtVersion;

      fetchRevision(versionToFetch, index, revisionList -> {
        revisions.addAll(revisionList);
        getRevisionSafe(index, callback);
      });
    } else {
      getRevisionSafe(index, callback);
    }


  }

  /**
   *
   * @param resultingVersion
   *          the resulting version where the first revision starts at
   * @param fetchCount
   *          number of revisions to return if possible
   * @param nextRevisionIndex
   *          the sequential index to set in the next fetched revision
   * @param callback
   *          callback to return revisions asynchronously
   */
  protected abstract void fetchRevision(HashedVersion resultingVersion,
      int nextRevisionIndex,
      RevisionListResult callback);

  protected abstract void fetchSnaphost(DocRevision revision, RawSnapshotResult callback);

  public DocHistory.Iterator getIterator() {
    return new DocHistory.Iterator();
  }

  public DocHistory.Iterator getIteratorAt(DocRevision revision) {
    DocHistory.Iterator iterator = new DocHistory.Iterator();
    iterator.revPointer = revision.revisionIndex;
    return iterator;
  }

  @JsIgnore
  public void getSnapshot(DocRevision revision, SnapshotResult callback) {
    fetchSnaphost(revision, rawSnapshot -> {
      try {
        if (callback != null)
          callback.result(DocOpUtil.docInitializationFromXml(rawSnapshot));
      } catch (XmlParseException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  /** Only for testing purposes */
  public DocRevision getUnsafe(int index) {
    return revisions.get(index);
  }

}
