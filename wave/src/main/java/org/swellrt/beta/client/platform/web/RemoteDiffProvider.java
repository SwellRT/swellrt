package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.impl.GetDiffDataOperation;
import org.swellrt.beta.client.operation.impl.GetDiffDataOperation.Response;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.wave.DiffData.WaveletDiffData;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * A diff provider that retrieves data from server.
 *
 */
public class RemoteDiffProvider implements DiffProvider {


  private final WaveId waveId;
  private final ServiceContext context;

  public RemoteDiffProvider(WaveId waveId, ServiceContext context) {
    this.context = context;
    this.waveId = waveId;
  }



  @Override
  public void getDiffs(WaveletId waveletId, String docId, HashedVersion version,
      com.google.gwt.core.client.Callback<WaveletDiffData, Exception> callback) {

    GetDiffDataOperation op = new GetDiffDataOperation(context);

    GetDiffDataOperation.Options options = new GetDiffDataOperation.Options(
        ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId),
        ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId),
        docId, version);

    op.execute(options, new Operation.Callback<GetDiffDataOperation.Response>() {

      @Override
      public void onError(SException exception) {
        callback.onFailure(exception);
      }

      @Override
      public void onSuccess(Response response) {
        callback.onSuccess(response);
      }
    });


  }

}
