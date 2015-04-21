package org.swellrt.client;


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






public class WaveManager {



  private final String seed;
  private final ParticipantId signedUserId;
  private final SchemaProvider schemaProvider;
  private final RemoteViewServiceMultiplexer channel;
  private final IdGenerator idGenerator;
  private final String localDomain;
  private final WaveStore waveStore;
  private final UnsavedDataListener dataListener;

  /**
   * private ProfileManager profileManager;
   *
   *
   *
   * private String localDomain;
   **/


  private final Map<WaveRef, WaveWrapper> waveWrappers;

  /**
   * It needs a proper Session object
   * 
   * @param waveStore
   * @param idGenerator
   * @param channel
   * @return
   */
  public static WaveManager create(WaveStore waveStore, IdGenerator idGenerator,
      RemoteViewServiceMultiplexer channel, UnsavedDataListener dataListener) {
    return new WaveManager(waveStore, Session.get().getDomain(),
        ParticipantId.ofUnsafe(Session.get().getAddress()), Session.get().getIdSeed(),
 new ConversationSchemas(), channel,
        idGenerator, dataListener);
  }


  public static WaveManager create(WaveStore waveStore, String localDomain,
      IdGenerator idGenerator, ParticipantId loggedInUser, String idSeed,
      RemoteViewServiceMultiplexer channel, UnsavedDataListener dataListener) {

    return new WaveManager(waveStore, localDomain, loggedInUser, idSeed,
        new ConversationSchemas(),
        channel, idGenerator, dataListener);
  }





  protected WaveManager(WaveStore waveStore, String localDomain, ParticipantId signedUserId,
      String seed,
      SchemaProvider schemaProvider,
 RemoteViewServiceMultiplexer channel,
      IdGenerator idGenerator, UnsavedDataListener dataListener) {

    this.waveStore = waveStore;
    this.signedUserId = signedUserId;
    this.seed = seed;
    this.waveWrappers = new HashMap<WaveRef, WaveWrapper>();
    this.schemaProvider = schemaProvider;
    this.channel = channel;
    this.idGenerator = idGenerator;
    this.localDomain = localDomain;
    this.dataListener = dataListener;
  }



  public WaveWrapper getWaveContentWrapper(WaveRef waveRef, boolean isNewWave) {

    return new WaveWrapper(waveRef, getChannel(), getIdGenerator(), getWaveStore(),
        getLocalDomain(), null, getSignedUserId(), isNewWave, dataListener);

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
