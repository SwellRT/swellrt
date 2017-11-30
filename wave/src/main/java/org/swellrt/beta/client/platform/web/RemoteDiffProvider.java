package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.GetDiffDataOperation;
import org.swellrt.beta.client.rest.operations.GetDiffDataOperation.Response;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.wave.DiffData;
import org.waveprotocol.wave.client.wave.DiffData.WaveletDiffData;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

import com.google.gwt.core.client.JavaScriptObject;

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

  public static class JsoWaveletDiffData extends JavaScriptObject
      implements GetDiffDataOperation.Response {

    @Override
    public DiffData[] get(String blipId) {
      return (DiffData[]) JsoView.as(this).getObjectUnsafe(blipId);
    }

  }


  @Override
  public void getDiffs(WaveletId waveletId, String docId, HashedVersion version,
      com.google.gwt.core.client.Callback<WaveletDiffData, Exception> callback) {

    GetDiffDataOperation.Options options = new GetDiffDataOperation.Options(
        ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId),
        ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId),
        docId, version);

    GetDiffDataOperation op = new GetDiffDataOperation(context, options,
        new ServiceOperation.Callback<GetDiffDataOperation.Response>() {

          @Override
          public void onError(SException exception) {
            callback.onFailure(exception);
          }

          @Override
          public void onSuccess(Response response) {
            // Force cast to JsoWaveletDiffData to access js internals.
            JsoWaveletDiffData jsoResponse = (JsoWaveletDiffData) response;
            callback.onSuccess(jsoResponse);
          }

    });


  }

}
