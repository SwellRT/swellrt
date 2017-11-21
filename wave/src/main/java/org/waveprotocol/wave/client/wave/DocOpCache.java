package org.waveprotocol.wave.client.wave;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.waveprotocol.wave.client.editor.playback.DocOpContext;
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
 * Cache of {@link DocOp} instances together with its context info.
 *
 * @author pablojan@gmail.com
 *
 */
public class DocOpCache {


  static class OpCache {

    public Map<DocOp, DocOpContext> data = new HashMap<DocOp, DocOpContext>();
    public long version = 0;

  }

  /** Only cache ops from allowed blip types */
  final private ReadableStringSet allowedBlipPrefixes;

  /** Map of caches with key = "waveletId/blipId" */
  final private Map<String, OpCache> cachesPerDoc = new HashMap<String, OpCache>();

  private boolean cacheSnapshotOn = false;

  /** build a key for map of caches */
  private static String buildDocKey(String waveletId, String blipId) {
    return waveletId + "/" + blipId;
  }


  public DocOpCache(ReadableStringSet allowedBlipPrefixes) {
    this.allowedBlipPrefixes = allowedBlipPrefixes;
  }

  /** get a cache for a particular document (blip) */
  private OpCache getCache(String waveletId, String blipId) {

    String docKey = buildDocKey(waveletId, blipId);
    if (!cachesPerDoc.containsKey(docKey))
      cachesPerDoc.put(docKey, new OpCache());

    return cachesPerDoc.get(docKey);
  }

  private boolean isValidBlip(String blipId) {
    return allowedBlipPrefixes.contains(IdUtil.getInitialToken(blipId));
  }

  /** add a DocOp to the cache wrapped in a WaveletOperation */
  public void add(String waveletId, WaveletOperation waveletOperation) {

    if (waveletOperation instanceof WaveletBlipOperation) {

      String blipId = ((WaveletBlipOperation) waveletOperation).getBlipId();

      // Don't cache ops from no relevant blips
      if (!isValidBlip(blipId))
        return;

      BlipOperation blipOp = ((WaveletBlipOperation) waveletOperation)
            .getBlipOp();

      if (!(blipOp instanceof BlipContentOperation))
        return;

      OpCache cache = getCache(waveletId, blipId);
      cache.data.put(((BlipContentOperation) blipOp).getContentOp(),
          new DocOpContext(blipOp.getContext()));
    }

  }

  /**
   * Add blips in this snapshot into the cache as {@link DocInitialization} ops.
   *
   * @param snapshot
   */
  public void add(ReadableWaveletData snapshot) {

    if (!cacheSnapshotOn)
      return;

    String waveletId = ModernIdSerialiser.INSTANCE.serialiseWaveletId(snapshot.getWaveletId());

    snapshot.getDocumentIds().forEach( docId -> {

      // Don't cache ops from no relevant blips
      if (isValidBlip(docId)) {

        ReadableBlipData doc = snapshot.getDocument(docId);

        DocInitialization op = doc.getContent().asOperation();
        long ver = doc.getLastModifiedVersion();

        OpCache cache = getCache(waveletId, docId);
        cache.data.put(op, new DocOpContext(doc.getLastModifiedTime(), doc.getAuthor(), ver,
            HashedVersion.unsigned(ver)));
        cache.version = ver;

      }

    });

  }

  /** Get a DocOp removing it from cache */
  public Optional<DocOpContext> fetch(String waveletId, String blipId, DocOp op) {
    OpCache cache = getCache(waveletId, blipId);
    return Optional.ofNullable(cache.data.get(op));
  }

  /** should we cache blips in snapshots? */
  public void enableSnapshotCache(boolean on) {
    this.cacheSnapshotOn = on;
  }

}
