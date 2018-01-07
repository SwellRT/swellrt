package org.waveprotocol.box.server.batch;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Comma separated list of wavelet names.
 *
 * @author pablo
 *
 */
public class StringWaveletSelector implements WaveletSelector {

  protected final static String TYPE_PREFIX = "string:";

  private final String inputStr;
  private WaveletName[] waveletNames;

  public StringWaveletSelector(String str) {
    this.inputStr = str;
  }

  private WaveletName[] buildWaveletNameArray() {

    if (waveletNames == null) {
      Preconditions.checkArgument(inputStr != null, "List of wavelet names is null");
      String[] names = inputStr.split(",");

      this.waveletNames = (WaveletName[]) Arrays.stream(names)
          .map(WaveletSelector::deserializeWaveletName)
          .filter(Objects::nonNull)
          .toArray();
    }
    return waveletNames;
  }

  @Override
  public Stream<WaveletName> getWaveletNames() {
    buildWaveletNameArray();
    return Arrays.stream(waveletNames);
  }

}
