package org.waveprotocol.wave.client.wave;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

/**
 * Tracks {@link DocOp}s together with its metadata (aka context). <br>
 * <p>
 * Use also to query op's metadata and blip/doc latest version.
 * </p>
 *
 * @author pablojan@gmail.com
 *
 */
public class WaveDocOpTracker {


  static class PerBlipOps {
    public Map<DocOp, DocOpContext> data = new HashMap<DocOp, DocOpContext>();
  }

  /** Only cache ops from allowed blip types */
  final private ReadableStringSet allowedBlipPrefixes;

  /** Map of caches with key = "waveletId/blipId" */
  final private Map<String, PerBlipOps> blipOps = new HashMap<String, PerBlipOps>();

  /** Map of latest versions of wavelets */
  final private Map<String, HashedVersion> waveletVersions = new HashMap<String, HashedVersion>();

  private boolean storeSnapshotOn = false;

  /** build a key for map of caches */
  private static String buildDocKey(String waveletId, String blipId) {
    return waveletId + "/" + blipId;
  }


  public WaveDocOpTracker(ReadableStringSet allowedBlipPrefixes) {
    this.allowedBlipPrefixes = allowedBlipPrefixes;
  }

  /** get a cache for a particular document (blip) */
  private PerBlipOps geBlipOps(String waveletId, String blipId) {

    String docKey = buildDocKey(waveletId, blipId);
    if (!blipOps.containsKey(docKey))
      blipOps.put(docKey, new PerBlipOps());

    return blipOps.get(docKey);
  }

  private boolean isValidBlip(String blipId) {
    return allowedBlipPrefixes.contains(IdUtil.getInitialToken(blipId));
  }

  private void updateWaveletVersion(String waveletId, HashedVersion version) {
    if (version == null)
      return;

    if (!waveletVersions.containsKey(waveletId)
        || version.getVersion() > waveletVersions.get(waveletId).getVersion()) {
      waveletVersions.put(waveletId, version);
    }
  }

  /** Track a WaveletOperation, maybe a DocOp or VersionUpdateOp */
  public void track(String waveletId, WaveletOperation waveletOperation) {

    if (waveletOperation instanceof WaveletBlipOperation) {

      String blipId = ((WaveletBlipOperation) waveletOperation).getBlipId();

      // Don't cache ops from no relevant blips
      if (!isValidBlip(blipId))
        return;

      BlipOperation blipOp = ((WaveletBlipOperation) waveletOperation)
            .getBlipOp();

      if (!(blipOp instanceof BlipContentOperation))
        return;

      PerBlipOps cache = geBlipOps(waveletId, blipId);
      cache.data.put(((BlipContentOperation) blipOp).getContentOp(),
          new DocOpContext(blipOp.getContext()));

    }

    updateWaveletVersion(waveletId, waveletOperation.getContext().getHashedVersion());

  }

  /**
   * Add a snapshot into the cache as {@link DocInitialization} op.
   * <p>
   * Call this method just right after loading a wavelet in order to track the
   * current version
   * </p>
   *
   * @param snapshot
   */
  public void track(ReadableWaveletData snapshot) {

    String waveletId = ModernIdSerialiser.INSTANCE.serialiseWaveletId(snapshot.getWaveletId());

    if (storeSnapshotOn) {

      snapshot.getDocumentIds().forEach(docId -> {

        // Don't cache ops from no relevant blips
        if (isValidBlip(docId)) {

          ReadableBlipData doc = snapshot.getDocument(docId);

          DocInitialization op = doc.getContent().asOperation();
          HashedVersion ver = snapshot.getHashedVersion();

          PerBlipOps cache = geBlipOps(waveletId, docId);

          cache.data.put(op,
              new DocOpContext(doc.getLastModifiedTime(), doc.getAuthor(), ver.getVersion(), ver));
        }

      });

    }

    updateWaveletVersion(waveletId, snapshot.getHashedVersion());

  }

  /** Get a DocOp removing it from cache */
  public Optional<DocOpContext> fetch(String waveletId, String blipId, DocOp op) {
    PerBlipOps cache = geBlipOps(waveletId, blipId);
    return Optional.ofNullable(cache.data.get(op));
  }

  /** Get the last wavelet version tracked for a blip */
  public Optional<HashedVersion> getVersion(String waveletId, String blipId) {
    return Optional.ofNullable(waveletVersions.get(waveletId));
  }

  /** should we cache blips in snapshots? */
  public void enableStoreSnapshot(boolean on) {
    this.storeSnapshotOn = on;
  }

}
