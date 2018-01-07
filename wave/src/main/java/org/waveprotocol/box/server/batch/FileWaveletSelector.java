package org.waveprotocol.box.server.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.Preconditions;

public class FileWaveletSelector implements WaveletSelector {

  protected final static String TYPE_PREFIX = "file:";

  private final String filePath;

  public FileWaveletSelector(String filePath) {
    this.filePath = filePath;
  }

  @Override
  public Stream<WaveletName> getWaveletNames() {

    Preconditions.checkArgument(filePath != null, "Null file path");
    try (Stream<String> lines = Files.lines(Paths.get(filePath))) {

      return lines.map(WaveletSelector::deserializeWaveletName);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

}
