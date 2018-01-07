package org.waveprotocol.box.server.batch;

public interface BatchOperation {

  String execute(BatchWaveletData data);

}
