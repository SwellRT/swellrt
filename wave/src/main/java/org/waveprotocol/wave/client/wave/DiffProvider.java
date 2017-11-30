package org.waveprotocol.wave.client.wave;

import org.waveprotocol.wave.client.wave.DiffData.WaveletDiffData;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

import com.google.gwt.core.client.Callback;

/**
 * Provides diff data for one Wave's text blip/documents.
 *
 */
public interface DiffProvider {

  public static interface Factory {

    DiffProvider get(WaveId waveId);

  }

  public static DiffProvider VOID_DIFF_PROVIDER = new DiffProvider() {

    @Override
    public void getDiffs(WaveletId waveletId, String docId, HashedVersion version,
        Callback<WaveletDiffData, Exception> callback) {

      callback.onSuccess(new WaveletDiffData() {

        @Override
        public DiffData[] get(String blipId) {
          return new DiffData[] {};
        }

      });

    }

  };

  public static DocDiffProvider VOID_DOC_DIFF_PROVIDER = new DocDiffProvider() {

    @Override
    public void getDiffs(Callback<DiffData[], Exception> callback) {
      callback.onSuccess(new DiffData[] {});
    }

  };

  public interface DocDiffProvider {

    void getDiffs(Callback<DiffData[], Exception> callback);

  }

  void getDiffs(WaveletId waveletId, String docId, HashedVersion version,
      Callback<WaveletDiffData, Exception> callback);

}
