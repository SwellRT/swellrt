package org.waveprotocol.mod.model.dummy;

import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;
import org.waveprotocol.wave.model.wave.WaveletListener;

import java.util.Map;
import java.util.Set;


public class ListModel implements SourcesEvents<ListModel.Listener> {

  public static final String WAVELET_ID_PREFIX = "dummy";
  public static final String WAVELET_ID = WAVELET_ID_PREFIX + IdUtil.TOKEN_SEPARATOR + "model";

  public interface Listener {

    void onAddParticipant(ParticipantId participant);

    void onRemoveParticipant(ParticipantId participant);

  }

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  private final ObservableWavelet wavelet;

  private static final String LIST_DOC_ID = "list+root";
  private static final String LIST_ITEM_TAG = "item";
  private static final String ITEM_VALUE_ATTR = "v";
  private final DocumentBasedElementList<Doc.E, BasicValue<String>, String> list;


  private final WaveletListener waveletListener = new WaveletListener(){

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


  };



  public static ListModel create(WaveContext wave, String domain, ParticipantId loggedInUser,
      boolean isNewWave, IdGeneratorDummy idGenerator) {

    WaveletId waveletId = WaveletId.of(domain, WAVELET_ID);
    ObservableWavelet wavelet = wave.getWave().getWavelet(waveletId);

    if (wavelet == null) {
      wavelet = wave.getWave().createWavelet(waveletId);
      wavelet.addParticipant(loggedInUser);
    }


    return new ListModel(wavelet, idGenerator);
  }


  protected ListModel(ObservableWavelet wavelet, IdGeneratorDummy idGenerator) {
    this.wavelet = wavelet;

    ObservableDocument listDoc = wavelet.getDocument(LIST_DOC_ID);
    DocEventRouter listDocRouter = DefaultDocEventRouter.create(listDoc);

    list =
        DocumentBasedElementList.create(listDocRouter, listDoc.getDocumentElement(), LIST_ITEM_TAG,
            new Factory<Doc.E, BasicValue<String>, String>() {

              @Override
              public BasicValue<String> adapt(DocumentEventRouter<? super E, E, ?> router, E element) {
                return DocumentBasedBasicValue.create(router, element, Serializer.STRING,
                    ITEM_VALUE_ATTR);
              }

              @Override
              public Initializer createInitializer(final String initialState) {

                return new Initializer() {

                  @Override
                  public void initialize(Map<String, String> target) {
                    target.put(ITEM_VALUE_ATTR, initialState);
                  }

                };
              }


            });


  }


  public ObservableElementList<BasicValue<String>, String> getList() {
    return list;
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


  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }


  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }


}
