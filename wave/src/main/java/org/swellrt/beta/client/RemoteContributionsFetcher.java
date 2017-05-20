package org.swellrt.beta.client;

import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.client.operation.impl.FetchContributionsOperation;
import org.swellrt.beta.client.operation.impl.FetchContributionsOperation.Response;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.editor.content.DocContributionsFetcher;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 *
 * @author pablojan@apache.org (Pablo Ojanguren)
 *
 */
public class RemoteContributionsFetcher implements DocContributionsFetcher {


  public static class RemoteFactory implements DocContributionsFetcher.Factory {

    final ServiceContext serviceContext;

    public RemoteFactory(ServiceContext serviceContext) {
      this.serviceContext = serviceContext;
    }

    @Override
    public DocContributionsFetcher create(WaveId waveId) {
      return new RemoteContributionsFetcher(serviceContext, waveId);
    }

  }

  final private ServiceContext serviceContext;
  final private WaveId waveId;


  protected RemoteContributionsFetcher(ServiceContext serviceContext, WaveId waveId) {
    super();
    this.serviceContext = serviceContext;
    this.waveId = waveId;
  }


  @Override
  public void fetchContributions(String waveletId, HashedVersion waveletVersion,
      Callback fetchCallback) {

    FetchContributionsOperation op = new FetchContributionsOperation(serviceContext);
    FetchContributionsOperation.Options options = new FetchContributionsOperation.Options() {

      @Override
      public String getWaveletId() {
        return waveletId;
      }

      @Override
      public String getBlipId() {
        return "all";
      }

      @Override
      public String getBase64HashVersion() {
        return  CharBase64.encodeWebSafe(waveletVersion.getHistoryHash(), true);
      }

      @Override
      public double getLongVersion() {
        return waveletVersion.getVersion();
      }

      @Override
      public String getWaveId() {
        return ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId);
      }

    };


    op.execute(options, new Operation.Callback<FetchContributionsOperation.Response>() {

      @Override
      public void onSuccess(Response response) {
        if (fetchCallback != null)
          fetchCallback.onSuccess(response);
      }

      @Override
      public void onError(SException exception) {
        if (fetchCallback != null)
          fetchCallback.onException(exception);
      }
    });

  }
}
