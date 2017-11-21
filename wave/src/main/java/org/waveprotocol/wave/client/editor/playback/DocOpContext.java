package org.waveprotocol.wave.client.editor.playback;


import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Context information of a doc op. It contains same info as the
 * {@link WaveletOperationContext} containing the {link DocOp}. Hence, use it
 * instead to avoid mixing semantic context.
 *
 * @author pablojan@apache.org
 *
 */
public final class DocOpContext {

  /*
   * public static DocOpContext invert(DocOpContext directDelta) {
   *
   * List<DocOp> reverseOps = new ArrayList<DocOp>();
   *
   * for (int i = directDelta.contentOps.size() - 1; i >= 0; i--) {
   * reverseOps.add(DocOpInverter.invert(directDelta.contentOps.get(i))); }
   *
   * return new DocOpContext(reverseOps, directDelta.timestamp,
   * directDelta.creator, -directDelta.versionIncrement,
   * HashedVersion.unsigned(directDelta.hashedVersion.getVersion()));
   *
   * }
   */


  /** Time at which an operation occurred. */
  private final long timestamp;

  /** The participant that caused an operation. */
  private final ParticipantId creator;

  /** Number of versions to increment after applying this operation. */
  private final long versionIncrement;

  /** Hashed version of the wavelet after applying this operation (optional). */
  private final HashedVersion hashedVersion;

  public DocOpContext(long timestamp, ParticipantId creator,
      long versionIncrement, HashedVersion hashedVersion) {
    super();
    this.timestamp = timestamp;
    this.creator = creator;
    this.versionIncrement = versionIncrement;
    this.hashedVersion = hashedVersion;
  }

  public DocOpContext(WaveletOperationContext waveletOpContext) {
    this.timestamp = waveletOpContext.getTimestamp();
    this.creator = waveletOpContext.getCreator();
    this.versionIncrement = waveletOpContext.getVersionIncrement();
    this.hashedVersion = waveletOpContext.getHashedVersion();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public ParticipantId getCreator() {
    return creator;
  }

  public long getVersionIncrement() {
    return versionIncrement;
  }

  public HashedVersion getHashedVersion() {
    return hashedVersion;
  }

}
