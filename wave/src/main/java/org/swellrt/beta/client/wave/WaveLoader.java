package org.swellrt.beta.client.wave;

import java.util.Set;

import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.concurrencycontrol.common.TurbulenceListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

public interface WaveLoader extends WaveStages {

  public interface Factory {

    WaveLoader create(WaveId waveId,
        RemoteViewServiceMultiplexer channel,
        IdGenerator idGenerator,
        String localDomain,
        Set<ParticipantId> participants,
        ParticipantId loggedInUser,
        UnsavedDataListener dataListener,
        TurbulenceListener turbulenceListener,
        DiffProvider diffProvider);

  }

  void destroy();

  ObservableWaveView getWave();

  String getLocalDomain();

  boolean isNewWave();

  ParticipantId getLoggedInUser();

  IdGenerator getIdGenerator();

  SWaveDocuments<? extends InteractiveDocument> getDocumentRegistry();

  boolean isClosed();


}