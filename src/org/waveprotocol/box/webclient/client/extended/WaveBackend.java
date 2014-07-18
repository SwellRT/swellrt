package org.waveprotocol.box.webclient.client.extended;


import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.box.webclient.client.Session;
import org.waveprotocol.box.webclient.search.WaveStore;
import org.waveprotocol.wave.client.extended.ContentWave;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.HashMap;
import java.util.Map;






public class WaveBackend {




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


  private final Map<WaveRef, ContentWave> waveWrappers;

  public static WaveBackend create(WaveStore waveStore, IdGenerator idGenerator,
      RemoteViewServiceMultiplexer channel) {
    return new WaveBackend(waveStore, ParticipantId.ofUnsafe(Session.get().getAddress()), Session
        .get()
        .getIdSeed(), new ConversationSchemas(), channel, idGenerator);
  }




  protected WaveBackend(WaveStore waveStore, ParticipantId signedUserId, String seed,
      SchemaProvider schemaProvider,
      RemoteViewServiceMultiplexer channel, IdGenerator idGenerator) {

    this.waveStore = waveStore;
    this.signedUserId = signedUserId;
    this.seed = seed;
    this.waveWrappers = new HashMap<WaveRef, ContentWave>();
    this.schemaProvider = schemaProvider;
    this.channel = channel;
    this.idGenerator = idGenerator;
    this.localDomain = Session.get().getDomain();
  }

  //
  // Manage Wave Wrappers
  //



  public ContentWave getWaveWrapper(WaveRef waveRef, boolean isNew) {

    ContentWave ww =
        new ContentWave(waveRef, channel, idGenerator, waveStore, localDomain, null, isNew);

    return ww;
  }




}
