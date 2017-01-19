package org.swellrt.beta.model.remote;

import org.swellrt.beta.model.SStatusEvent;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.WaveletListener;

/**
 * A wavelet listener dispatching events to a SObjectRemote.
 * This listener implementation is separated from SObjectRemote
 * for shake of clarity.
 * <p>
 * TODO Wavelet and WaveletListener interfaces should been adapted to
 * SwellRT general purpose
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SWaveletListener implements WaveletListener {

  
  private final SObjectRemote sobject;
  
  
  public SWaveletListener(SObjectRemote sobject) {
    super();
    this.sobject = sobject;
  }

  @Override
  public void onParticipantAdded(ObservableWavelet wavelet, ParticipantId participant) {
    sobject.onStatusEvent(new SStatusEvent(sobject.getId(), participant, false));
  }

  @Override
  public void onParticipantRemoved(ObservableWavelet wavelet, ParticipantId participant) {
    sobject.onStatusEvent(new SStatusEvent(sobject.getId(), participant, true));
  }

  @Override
  public void onLastModifiedTimeChanged(ObservableWavelet wavelet, long oldTime, long newTime) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBlipAdded(ObservableWavelet wavelet, Blip blip) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBlipRemoved(ObservableWavelet wavelet, Blip blip) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBlipSubmitted(ObservableWavelet wavelet, Blip blip) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBlipTimestampModified(ObservableWavelet wavelet, Blip blip, long oldTime,
      long newTime) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBlipVersionModified(ObservableWavelet wavelet, Blip blip, Long oldVersion,
      Long newVersion) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBlipContributorAdded(ObservableWavelet wavelet, Blip blip,
      ParticipantId contributor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBlipContributorRemoved(ObservableWavelet wavelet, Blip blip,
      ParticipantId contributor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onHashedVersionChanged(ObservableWavelet wavelet, HashedVersion oldHashedVersion,
      HashedVersion newHashedVersion) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip) {
    // TODO Auto-generated method stub

  }

}
