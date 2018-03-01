package org.swellrt.beta.client.platform.web;

import java.util.Arrays;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.ServiceDeps;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.GetContributionsOperation;
import org.swellrt.beta.client.rest.operations.GetContributionsOperation.Response;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.wave.DiffData;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

import com.google.gwt.core.client.Callback;

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
      Callback<DiffData, Exception> callback) {

    GetContributionsOperation.Options options = new GetContributionsOperation.Options(
        ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId),
        ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId),
        docId, version);

    GetContributionsOperation op = new GetContributionsOperation(context, options,
        new ServiceOperation.Callback<GetContributionsOperation.Response>() {

          @Override
          public void onError(SException exception) {
            callback.onFailure(exception);
          }

          @Override
          public void onSuccess(Response response) {

            Arrays.<DiffData> stream(response.getContrib()).filter((DiffData diffData) -> {
              return diffData.getDocId().equals(docId);
            }).findFirst().ifPresent(diffData -> {
              callback.onSuccess(diffData);
            });
            ;

          }

    });

    ServiceDeps.remoteOperationExecutor.execute(op);
  }

}
