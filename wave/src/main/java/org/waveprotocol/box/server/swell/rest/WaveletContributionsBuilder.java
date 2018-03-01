package org.waveprotocol.box.server.swell.rest;

import java.io.IOException;

import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.swell.WaveletContributions;
import org.waveprotocol.box.server.swell.WaveletContributions.BlipContributions;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.util.logging.Log;

import com.google.gson.stream.JsonWriter;

public class WaveletContributionsBuilder {

  private static final Log LOG = Log.get(WaveletContributionsBuilder.class);

  public static void build(WaveletProvider waveletProvider, WaveletName waveletName,
      HashedVersion version, JsonWriter jw)
      throws WaveServerException, IOException {

    final WaveletContributions waveletContribs = new WaveletContributions(waveletName);

    waveletProvider.getHistory(waveletName,
        RestUtils.HashVersionFactory.createVersionZero(waveletName), version,
        new Receiver<TransformedWaveletDelta>() {

          @Override
          public boolean put(TransformedWaveletDelta delta) {
            waveletContribs.apply(delta);
            return true;
          }
        });

    //
    // Output as JSON
    //

    jw.beginArray();

    waveletContribs.getBlipContributions().forEach((pair) -> {
      try {

        jw.beginObject();

        jw.name("docId");
        jw.value(pair.getKey());

        jw.name("ranges");
        serializeBlipContributions(jw, pair.getValue());

        jw.endObject();

      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    jw.endArray();

    LOG.fine("Wavelet contributions built for " + waveletName.toString() + ", version "
        + version.getVersion());

  }

  protected static void serializeBlipContributions(JsonWriter jw, BlipContributions contribs)
      throws IOException {

    jw.beginArray();

    contribs.getIntervals().forEach(interval -> {

      try {

        jw.beginObject(); // interval

        jw.name("start").value(interval.start());
        jw.name("end").value(interval.end());

        jw.name("values");
        jw.beginObject(); // values
        interval.annotations().each(new ProcV<Object>() {

          @Override
          public void apply(String key, Object value) {
            try {
              jw.name(key).value(value.toString());
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        });
        jw.endObject(); // values

        jw.endObject(); // interval

      } catch (IOException e) {
        e.printStackTrace();
      }

    });

    jw.endArray();

  }
}
