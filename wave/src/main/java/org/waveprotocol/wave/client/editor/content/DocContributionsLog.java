package org.waveprotocol.wave.client.editor.content;

import java.util.HashMap;
import java.util.Map;

import org.waveprotocol.wave.client.editor.content.DocContributionsFetcher.WaveletContributions;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

/**
 * Log document operations received in wave's wavelets in order to query
 * wavelet op's metadata (as author and version) later.
 * <p><br/>
 * This class is intended to be use by {@see DiffHighlightingFilter}
 * in order to display author names for each diff text in the whole document.
 *
 * @author pablojan@apache.org (Pablo Ojanguren)
 *
 */
public class DocContributionsLog {

  static class WaveletIdVersion {
    public WaveletIdVersion(String waveletId, String hashVersion) {
      super();
      this.waveletId = waveletId;
      this.hashVersion = hashVersion;
    }
    public String waveletId;
    public String hashVersion;

  }

  static class WaveletOpLog {
    public Map<DocOp, WaveletOperationContext> opContextMap = new HashMap<DocOp, WaveletOperationContext>();
    public HashedVersion lastVersion = null;
  }

  final Map<String, WaveletOpLog> perWaveletLog = new HashMap<String, WaveletOpLog>();
  final ReadableStringSet acceptedBlipPrefixes;
  final DocContributionsFetcher contributionsFetcher;

  final Map<WaveletIdVersion, DocContributionsFetcher.WaveletContributions> baseContributions
    = new HashMap<WaveletIdVersion, DocContributionsFetcher.WaveletContributions>();

  public DocContributionsLog(ReadableStringSet acceptedBlipPrefixes, DocContributionsFetcher contributionsFetcher) {
    if (acceptedBlipPrefixes == null)
      acceptedBlipPrefixes = CollectionUtils.createStringSet();
    this.acceptedBlipPrefixes = acceptedBlipPrefixes;
    this.contributionsFetcher = contributionsFetcher;
  }

  protected WaveletOpLog getOrCreateWaveletLog(String waveletId) {
    if (!perWaveletLog.containsKey(waveletId))
      perWaveletLog.put(waveletId, new WaveletOpLog());
    return perWaveletLog.get(waveletId);
  }

  /**
   * Track a wavelet operation.
   *
   * @param waveletId
   * @param op
   */
  public void register(String waveletId, WaveletOperation op) {
    WaveletOpLog log = getOrCreateWaveletLog(waveletId);

    if (op instanceof WaveletBlipOperation) {
      String blipId = ((WaveletBlipOperation) op).getBlipId();

      // Filter out non swellrt text blips
      if (acceptedBlipPrefixes.contains(IdUtil.getInitialToken(blipId))) {
        BlipContentOperation blipOp = (BlipContentOperation) ((WaveletBlipOperation) op).getBlipOp();
        log.opContextMap.put(blipOp.getContentOp(), blipOp.getContext());
      }
    }

    log.lastVersion = op.getContext().getHashedVersion();

  }

  /**
   * Track a wavelet snapshot. This method should be called before any
   * operation register.
   *
   * @param snapshot wavelet's snapshot
   */
  public void registerSnapshot(ReadableWaveletData snapshot) {
    if (snapshot == null) return;
    String waveletId = ModernIdSerialiser.INSTANCE.serialiseWaveletId(snapshot.getWaveletId());
    WaveletOpLog log = getOrCreateWaveletLog(waveletId);
    log.lastVersion = snapshot.getHashedVersion();
  }

  public WaveletOperationContext peekOpContext(String waveletId, String blipId, DocOp docOp) {

    WaveletOpLog waveletOpLog = perWaveletLog.get(waveletId);

    WaveletOperationContext ctx = waveletOpLog.opContextMap.get(docOp);
    if (ctx != null) {
      waveletOpLog.opContextMap.remove(docOp);
    }
    return ctx;
  }

  public HashedVersion getWaveletLastVersion(String waveletId) {
    WaveletOpLog waveletOpLog = perWaveletLog.get(waveletId);
    if (waveletOpLog == null) return null;

    return waveletOpLog.lastVersion;
  }

  /**
   * Fetch all contributions for all documents in a wavelet at specific version.
   *
   * @param waveletId
   * @param blipId
   */
  public void fetchContributions(String waveletId, HashedVersion waveletVersion, DocContributionsFetcher.Callback callback) {

    if (contributionsFetcher == null) return;

    final WaveletIdVersion idversion = new WaveletIdVersion(waveletId, waveletVersion.toString());

    if (baseContributions.containsKey(idversion))
      if (callback != null) {
        callback.onSuccess(baseContributions.get(idversion));
        return;
      }
    contributionsFetcher.fetchContributions(waveletId, waveletVersion, new DocContributionsFetcher.Callback() {

      @Override
      public void onSuccess(WaveletContributions waveletContributions) {
        baseContributions.put(idversion, waveletContributions);
        if (callback != null)
          callback.onSuccess(waveletContributions);
      }

      @Override
      public void onException(Exception e) {
        if (callback != null)
          callback.onException(e);

      }

    });

  }

}
