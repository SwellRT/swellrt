package org.waveprotocol.box.server.batch;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.inject.Inject;

public class BatchExecutor {

  private final WaveletProvider waveletProvider;

  @Inject
  public BatchExecutor(WaveletProvider waveletProvider) {
    this.waveletProvider = waveletProvider;
  }

  private Stream<WaveletName> getWaveletNames(String waveletSelector) {

    Preconditions.checkArgument(waveletSelector != null, "Wavelet Selector expression is null");

    if (waveletSelector.startsWith(FileWaveletSelector.TYPE_PREFIX)) {
      String filePath = waveletSelector.substring(FileWaveletSelector.TYPE_PREFIX.length());
      return new FileWaveletSelector(filePath).getWaveletNames();

    } else if (waveletSelector.startsWith(StringWaveletSelector.TYPE_PREFIX)) {
      String str = waveletSelector.substring(StringWaveletSelector.TYPE_PREFIX.length());
      return new StringWaveletSelector(str).getWaveletNames();
    }

    return Arrays.stream(new WaveletName[] {});
  }

  private Optional<BatchOperation> getBatchOperation(String operationClassName) {

    try {
      @SuppressWarnings("unchecked")
      Class<? extends BatchOperation> batchOperationClass = (Class<? extends BatchOperation>) Class
          .forName(operationClassName);

      return Optional.of(batchOperationClass.newInstance());

    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  private BatchWaveletData retrieveWaveletData(WaveletName waveletName) {

    CommittedWaveletSnapshot waveletSnapshot;
    try {
      waveletSnapshot = waveletProvider.getSnapshot(waveletName);
      return new BatchWaveletData(waveletSnapshot.snapshot, waveletSnapshot.committedVersion);

    } catch (WaveServerException e) {
      System.err.println(waveletName.toString() + " ERROR " + e.getMessage());
    }

    return null;

  }

  /**
   * Execute a set of batch operations over a set of wavelets.
   *
   * @param operations
   * @param waveletSelectors
   */
  public void execute(String operationClassName, String waveletSelector) {

    Optional<BatchOperation> optBatchOperation = getBatchOperation(operationClassName);
    if (!optBatchOperation.isPresent()) {
      System.err.println("Do you provide a valid batch operation class name?");
      return;
    }

    Stream<WaveletName> waveletNames = getWaveletNames(waveletSelector);
    BatchOperation op = optBatchOperation.get();

    waveletNames.map(this::retrieveWaveletData).filter(Objects::nonNull).map(op::execute)
        .forEach(s -> {
          System.err.println(s);
        });



  }

}
