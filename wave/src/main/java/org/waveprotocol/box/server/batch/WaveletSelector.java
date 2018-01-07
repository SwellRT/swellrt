package org.waveprotocol.box.server.batch;

import java.util.stream.Stream;

import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletName;

public interface WaveletSelector {

  public static WaveletName deserializeWaveletName(String s) {
    try {
      return ModernIdSerialiser.INSTANCE.deserialiseWaveletName(s);
    } catch (InvalidIdException e) {
      e.printStackTrace();
    }
    return null;
  }

  Stream<WaveletName> getWaveletNames();
}
