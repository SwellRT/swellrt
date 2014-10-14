package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.mod.model.p2pvalue.docbased.DocBasedCommunity;
import org.waveprotocol.mod.model.p2pvalue.docbased.DocBasedProject;
import org.waveprotocol.mod.model.p2pvalue.id.IdGeneratorP2Pvalue;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;
import org.waveprotocol.wave.model.wave.WaveletListener;

import java.util.ArrayList;
import java.util.List;

public class CommunityWavelet implements SourcesEvents<CommunityWavelet.Listener> {


  public interface Listener {

    void onAddParticipant(ParticipantId participant);

    void onRemoveParticipant(ParticipantId participant);

    void onProjectAdded(String projectId);

    void onProjectRemoved(String projectId);

  }


  public static final String WAVELET_ID = "community" + IdUtil.TOKEN_SEPARATOR + "root";

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private final ObservableWavelet wavelet;
  private final DocBasedCommunity community;
  private final WaveletListener waveletListener = new WaveletListener() {

    @Override
    public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {
    }

    @Override
    public void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip) {
    }

    @Override
    public void onParticipantRemoved(ObservableWavelet wavelet, ParticipantId participant) {
      for (Listener l : listeners)
        l.onRemoveParticipant(participant);
    }

    @Override
    public void onParticipantAdded(ObservableWavelet wavelet, ParticipantId participant) {
      for (Listener l : listeners)
        l.onAddParticipant(participant);
    }

    @Override
    public void onLastModifiedTimeChanged(ObservableWavelet wavelet, long oldTime, long newTime) {
    }

    @Override
    public void onHashedVersionChanged(ObservableWavelet wavelet, HashedVersion oldHashedVersion,
        HashedVersion newHashedVersion) {
    }

    @Override
    public void onBlipVersionModified(ObservableWavelet wavelet, Blip blip, Long oldVersion,
        Long newVersion) {
    }

    @Override
    public void onBlipTimestampModified(ObservableWavelet wavelet, Blip blip, long oldTime,
        long newTime) {
    }

    @Override
    public void onBlipSubmitted(ObservableWavelet wavelet, Blip blip) {
    }

    @Override
    public void onBlipContributorRemoved(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
    }

    @Override
    public void onBlipContributorAdded(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
    }

    @Override
    public void onBlipAdded(ObservableWavelet wavelet, Blip blip) {
      if (IdUtil.getInitialToken(blip.getId()).equals(IdGeneratorP2Pvalue.PROJECT_DOC_PREFIX)) {
        for (Listener l : listeners)
          l.onProjectAdded(blip.getId());
      }
    }

    @Override
    public void onBlipRemoved(ObservableWavelet wavelet, Blip blip) {
      if (IdUtil.getInitialToken(blip.getId()).equals(IdGeneratorP2Pvalue.PROJECT_DOC_PREFIX)) {
        for (Listener l : listeners)
          l.onProjectRemoved(blip.getId());
      }
    }

  };


  /**
   * Create or retrieve a Community Wavelet.
   *
   * @param wave Container Wave
   * @param domain Domain of the Wave
   * @param loggedInUser Current user accesing the Wave
   * @param isNewWave true if Wave/Wavelet are brand new (not stored in the Wave
   *        provider).
   * @return the CommunityWavelet object
   */
  public static CommunityWavelet create(WaveContext wave, String domain,
      ParticipantId loggedInUser, boolean isNewWave) {

    WaveletId waveletId = WaveletId.of(domain, WAVELET_ID);

    ObservableWavelet wavelet = wave.getWave().getWavelet(waveletId);

    if (wavelet == null) {
      wavelet = wave.getWave().createWavelet(waveletId);
      wavelet.addParticipant(loggedInUser);
    }

    CommunityWavelet communityWavelet = new CommunityWavelet(wavelet);

    return communityWavelet;
  }


  /**
   * Constructor.
   *
   * @param wavelet The Wavelet supporting a community.
   */
  CommunityWavelet(ObservableWavelet wavelet) {
    this.wavelet = wavelet;
    this.wavelet.addListener(waveletListener);
    this.community = DocBasedCommunity.create(wavelet);
  }


  public void addParticipant(ParticipantId participant) {
    wavelet.addParticipant(participant);
  }

  public void removeParticipant(ParticipantId participant) {
    wavelet.removeParticipant(participant);
  }

  public Community getCommunity() {
    return community;
  }

  /**
   * Return list of Projects in the Community. Project objects are built in each
   * call of this method. Avoid call it several times.
   *
   *
   * @return Set of Project Id's
   */
  public Iterable<Project> getProjects() {

    List<Project> list = new ArrayList<Project>();

    for (String docId : wavelet.getDocumentIds()) {
      if (IdUtil.getInitialToken(docId).equals(IdGeneratorP2Pvalue.PROJECT_DOC_PREFIX))
        list.add(DocBasedProject.create(wavelet, docId));
    }

    return list;
  }




  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

}
