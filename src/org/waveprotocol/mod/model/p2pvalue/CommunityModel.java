package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.mod.model.p2pvalue.docbased.DocBasedCommunity;
import org.waveprotocol.mod.model.p2pvalue.docindex.DocBasedDocIndex;
import org.waveprotocol.mod.model.p2pvalue.id.IdGeneratorCommunity;
import org.waveprotocol.wave.model.document.ObservableDocument;
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

import java.util.Set;

public class CommunityModel implements SourcesEvents<CommunityModel.Listener> {

  public static final String WAVELET_ID_PREFIX = "community";
  public static final String WAVELET_ID = WAVELET_ID_PREFIX + IdUtil.TOKEN_SEPARATOR + "root";


  public interface Listener {

    void onAddParticipant(ParticipantId participant);

    void onRemoveParticipant(ParticipantId participant);

  }


  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  private final ObservableWavelet wavelet;
  private final DocBasedDocIndex docIndex;
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
    }

    @Override
    public void onBlipRemoved(ObservableWavelet wavelet, Blip blip) {
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
  public static CommunityModel create(WaveContext wave, String domain,
      ParticipantId loggedInUser, boolean isNewWave, IdGeneratorCommunity idGenerator) {

    WaveletId waveletId = WaveletId.of(domain, WAVELET_ID);

    ObservableWavelet wavelet = wave.getWave().getWavelet(waveletId);

    if (wavelet == null) {
      wavelet = wave.getWave().createWavelet(waveletId);
      wavelet.addParticipant(loggedInUser);
    }

    CommunityModel communityWavelet = new CommunityModel(wavelet, idGenerator);

    return communityWavelet;
  }


  /**
   * Constructor.
   *
   * @param wavelet The Wavelet supporting a community.
   */
  CommunityModel(ObservableWavelet wavelet, IdGeneratorCommunity idGenerator) {
    this.wavelet = wavelet;
    this.wavelet.addListener(waveletListener);

    // This is the only place where wavelet.getDocument() can be used
    // Following code must manage wavelet's docs using the DocIndex interface
    docIndex = DocBasedDocIndex.create(wavelet.getDocument(DocBasedDocIndex.DOC_ID));
    docIndex.initialize(wavelet, idGenerator);

    ObservableDocument communityDoc = docIndex.getDocument(DocBasedCommunity.DOC_ID);

    if (communityDoc != null) {
      this.community = DocBasedCommunity.create(communityDoc);
      this.community.setDocIndex(DocBasedCommunity.DOC_ID, docIndex);
    } else {
      communityDoc = docIndex.createDocumentWithId(DocBasedCommunity.DOC_ID);
      this.community = DocBasedCommunity.create(communityDoc);
      this.community.setDocIndex(DocBasedCommunity.DOC_ID, docIndex);
    }


  }


  public Set<ParticipantId> getParticipants() {
    return wavelet.getParticipantIds();
  }

  public void addParticipant(String address) {
    wavelet.addParticipant(ParticipantId.ofUnsafe(address));
  }

  public void removeParticipant(String address) {
    wavelet.removeParticipant(ParticipantId.ofUnsafe(address));
  }

  public Community getCommunity() {
    return community;
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
