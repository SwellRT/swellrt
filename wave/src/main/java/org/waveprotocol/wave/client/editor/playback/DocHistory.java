package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.version.HashedVersion;

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
public abstract class DocHistory {

  @FunctionalInterface
  public interface RevisionResult {
    void result(DocRevision revision);
  }

  @FunctionalInterface
  public interface MultipleRevisionResult {
    void result(List<DocRevision> revisionArray);
  }

  @FunctionalInterface
  public interface SnapshotResult {
    void result(DocInitialization snapshot);
  }

  @FunctionalInterface
  interface RawSnapshotResult {
    void result(String xmlSnapshot);
  }

  @FunctionalInterface
  public interface DocOpResult {
    public void result(DocOp[] ops);
  }

  public class Iterator {

    /**
     * current revision of the iterator
     */
    private int revPointer = -1;

    public boolean hasPrev() {
      return revisions.isEmpty()
          || (revPointer >= 0 && revisions.get(revPointer).appliedAtVersion.getVersion() != 0);
    }

    public boolean hasNext() {
      return revPointer > 0;
    }

    public void next(RevisionResult callback) {

      if (!hasNext())
        return;

      getRevision(--revPointer, callback);
    }

    public void prev(RevisionResult callback) {

      if (!hasPrev())
        return;

      getRevision(++revPointer, callback);

    }

    public void current(RevisionResult callback) {
      getRevision(revPointer, callback);
    }


    public void reset() {
      this.revPointer = 0;
    }

  }

  /** List of document revisions, newest version first (descending sort) */
  private final List<DocRevision> revisions = new ArrayList<DocRevision>();

  private final HashedVersion topVersion;

  public DocHistory(HashedVersion topVersion) {
    this.topVersion = topVersion;
  }

  protected DocRevision getLastRevision() {

    return revisions.get(revisions.size() - 1);
  }

  /** Gets a revision, fetching it remotely if necessary */
  private void getRevision(int index, RevisionResult callback) {

    if (revisions.size() <= index) {

      HashedVersion versionToFetch = topVersion;
      if (!revisions.isEmpty())
        versionToFetch = getLastRevision().appliedAtVersion;

      fetchRevision(versionToFetch, 10, index, revisionList -> {
        revisions.addAll(revisionList);
        callback.result(revisions.get(index));
      });
    } else {
      callback.result(revisions.get(index));
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
  protected abstract void fetchRevision(HashedVersion resultingVersion, int fetchCount,
      int nextRevisionIndex,
      MultipleRevisionResult callback);

  protected abstract void fetchSnaphost(DocRevision revision, RawSnapshotResult callback);

  protected abstract void fetchOps(DocRevision revision, DocOpResult callback);

  public DocHistory.Iterator getIterator() {
    return new DocHistory.Iterator();
  }

  public DocHistory.Iterator getIteratorAt(DocRevision revision) {
    DocHistory.Iterator iterator = new DocHistory.Iterator();
    iterator.revPointer = revision.revisionIndex;
    return iterator;
  }

  public void getSnapshot(DocRevision revision, SnapshotResult callback) {
    fetchSnaphost(revision, rawSnapshot -> {
      try {
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
