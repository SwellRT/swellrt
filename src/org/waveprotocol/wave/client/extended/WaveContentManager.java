package org.waveprotocol.wave.client.extended;


import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.box.webclient.client.Session;
import org.waveprotocol.box.webclient.search.WaveStore;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.HashMap;
import java.util.Map;






public class WaveContentManager {




  public class UnsavedDataListenerProxy implements UnsavedDataListener {


    private UnsavedDataListener actualListener = null;

    public void setActualUnsavedDataListener(UnsavedDataListener actualListener) {
      this.actualListener = actualListener;
    }

    @Override
    public void onUpdate(UnsavedDataInfo unsavedDataInfo) {
      if (actualListener != null) this.actualListener.onUpdate(unsavedDataInfo);

    }

    @Override
    public void onClose(boolean everythingCommitted) {
      if (actualListener != null) this.actualListener.onClose(everythingCommitted);
    }


  }


  private final String seed;
  private final ParticipantId signedUserId;
  private final SchemaProvider schemaProvider;
  private final RemoteViewServiceMultiplexer channel;
  private final IdGenerator idGenerator;
  private final String localDomain;
  private final WaveStore waveStore;

  /**
   * private ProfileManager profileManager;
   *
   *
   *
   * private String localDomain;
   **/


  private final Map<WaveRef, WaveContentWrapper> waveWrappers;

  /**
   * It needs a proper Session object
   * 
   * @param waveStore
   * @param idGenerator
   * @param channel
   * @return
   */
  public static WaveContentManager create(WaveStore waveStore, IdGenerator idGenerator,
      RemoteViewServiceMultiplexer channel) {
    return new WaveContentManager(waveStore, Session.get().getDomain(),
        ParticipantId.ofUnsafe(Session.get().getAddress()), Session.get().getIdSeed(),
        new ConversationSchemas(), channel, idGenerator);
  }


  public static WaveContentManager create(WaveStore waveStore, String localDomain,
      IdGenerator idGenerator, ParticipantId loggedInUser, String idSeed,
      RemoteViewServiceMultiplexer channel) {

    return new WaveContentManager(waveStore, localDomain, loggedInUser, idSeed,
        new ConversationSchemas(),
        channel, idGenerator);
  }





  protected WaveContentManager(WaveStore waveStore, String localDomain, ParticipantId signedUserId,
      String seed,
      SchemaProvider schemaProvider,
      RemoteViewServiceMultiplexer channel, IdGenerator idGenerator) {

    this.waveStore = waveStore;
    this.signedUserId = signedUserId;
    this.seed = seed;
    this.waveWrappers = new HashMap<WaveRef, WaveContentWrapper>();
    this.schemaProvider = schemaProvider;
    this.channel = channel;
    this.idGenerator = idGenerator;
    this.localDomain = localDomain;
  }



  public WaveContentWrapper getWaveContentWrapper(WaveRef waveRef, boolean isNewWave) {

    return new WaveContentWrapper(waveRef, getChannel(), getIdGenerator(), getWaveStore(),
        getLocalDomain(), null, getSignedUserId(), isNewWave);

  }

  protected String getSeed() {
    return seed;
  }


  protected ParticipantId getSignedUserId() {
    return signedUserId;
  }


  protected RemoteViewServiceMultiplexer getChannel() {
    return channel;
  }


  protected IdGenerator getIdGenerator() {
    return idGenerator;
  }


  protected String getLocalDomain() {
    return localDomain;
  }


  protected WaveStore getWaveStore() {
    return waveStore;
  }


}
