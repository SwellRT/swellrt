package org.swellrt.beta.client;

import org.swellrt.beta.client.ServiceFrontend.AsyncResponse;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.account.group.Group;
import org.waveprotocol.wave.model.account.group.ReadableGroup;
import org.waveprotocol.wave.model.account.group.WaveletBasedGroup;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import com.google.common.util.concurrent.FutureCallback;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "DefaultGroupsFrontend")
public class DefaultGroupFrontend implements GroupsFrontend {


  private final ServiceContext context;

  protected DefaultGroupFrontend(ServiceContext context) {
    this.context = context;
  }



  @Override
  public void open(ParticipantId groupId, AsyncResponse<Group> callback) throws SException {

    Preconditions.checkArgument(groupId != null && groupId.isGroup(), "Invalid group id");
    context.check();

    WaveletId groupWaveletId = WaveletId.of(groupId.getDomain(), groupId.getName());
    context.getMetadataWave(groupId.getDomain(), new FutureCallback<ObservableWaveView>() {

      @Override
      public void onSuccess(ObservableWaveView wave) {
        ObservableWavelet groupWavelet = wave.getWavelet(groupWaveletId);
        if (groupWavelet != null) {

          if (!groupWavelet.getParticipantIds()
              .contains(context.getServiceSession().getParticipantId())) {
            callback.onError(new SException(SException.NOT_AUTHORIZED));
            return;
          }

          callback.onSuccess(WaveletBasedGroup.create(groupWavelet));
        } else {
          callback.onError(new SException(SException.GROUP_NOT_FOUND));
        }

      }

      @Override
      public void onFailure(Throwable t) {
        callback.onError(new SException(SException.OPERATION_EXCEPTION, t));
      }

    });

  }

  @Override
  public void create(ParticipantId groupId, AsyncResponse<Group> callback) throws SException {

    Preconditions.checkArgument(groupId != null && groupId.isGroup(), "Invalid group id");
    context.check();

    WaveletId groupWaveletId = WaveletId.of(groupId.getDomain(), groupId.getName());
    context.getMetadataWave(groupId.getDomain(), new FutureCallback<ObservableWaveView>() {

      @Override
      public void onSuccess(ObservableWaveView wave) {
        ObservableWavelet groupWavelet = wave.getWavelet(groupWaveletId);
        if (groupWavelet != null) {
          callback.onError(new SException(SException.GROUP_ALREADY_EXISTS));
          return;
        } else {

          groupWavelet = wave.createWavelet(groupWaveletId);
          groupWavelet.addParticipant(context.getServiceSession().getParticipantId());
          callback.onSuccess(WaveletBasedGroup.create(groupWavelet));
        }

      }

      @Override
      public void onFailure(Throwable t) {
        callback.onError(new SException(SException.OPERATION_EXCEPTION, t));
      }

    });


  }

  @Override
  public void get(AsyncResponse<ReadableGroup[]> groupIds) throws SException {
    // TODO Auto-generated method stub

  }

  @Override
  public void delete(ParticipantId groupId) {
    // TODO Auto-generated method stub

  }

}
