package org.swellrt.android.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.swellrt.android.service.scheduler.OptimalGroupingScheduler;
import org.swellrt.android.service.scheduler.SchedulerInstance;
import org.swellrt.android.service.wave.WaveDocuments;
import org.swellrt.android.service.wave.client.common.util.ClientPercentEncoderDecoder;
import org.swellrt.android.service.wave.client.concurrencycontrol.LiveChannelBinder;
import org.swellrt.android.service.wave.client.concurrencycontrol.MuxConnector;
import org.swellrt.android.service.wave.client.concurrencycontrol.MuxConnector.Command;
import org.swellrt.android.service.wave.client.concurrencycontrol.WaveletOperationalizer;
import org.swellrt.model.generic.Model;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexerImpl;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexerImpl.LoggerContext;
import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannelFactory;
import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannelImpl;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListenerFactory;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDataDocumentImpl;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.document.indexed.IndexedDocumentImpl;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.Cancellable;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.CollectiveScheduler;
import org.waveprotocol.wave.model.util.Scheduler;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletConfigurator;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletFactory;
import org.waveprotocol.wave.model.waveref.WaveRef;

import com.google.common.base.Preconditions;

public class WaveLoader {


  public class WaveLoaderTask extends TimerTask {

    private org.waveprotocol.wave.model.util.Scheduler.Command command;

    public WaveLoaderTask(org.waveprotocol.wave.model.util.Scheduler.Command command) {
      this.command = command;
    }


    @Override
    public void run() {
      command.execute();

    }

    public Cancellable getCancellable() {

      return new Cancellable() {

        @Override
        public void cancel() {
          WaveLoaderTask.this.cancel();
        }

      };

    }

  };

  private boolean isClosed = true;

  // Provided objects

  private final WaveRef waveRef;
  private final boolean isNewWave;
  private final Set<ParticipantId> otherParticipants;
  private IdGenerator idGenerator;
  private ParticipantId signedInuser;

  private final RemoteViewServiceMultiplexer channel;
  private final UnsavedDataListener unsavedDataListener;

  private WaveContext waveContext;


  private Timer timer;
  private CollectiveScheduler rpcScheduler;

  // Wave stack.
  private WaveViewData waveData;

  private WaveDocuments<CcDataDocumentImpl> documentRegistry;
  private WaveletOperationalizer wavelets;
  private WaveViewImpl<OpBasedWavelet> wave;
  private MuxConnector connector;

  // Model objects
  private ObservableConversationView conversations;

  // private ObservableSupplementedWave supplement;


  public static WaveLoader create(boolean isNewWave, WaveRef waveRef,
      RemoteViewServiceMultiplexer channel, ParticipantId participant,
      Set<ParticipantId> otherParticipants,
      IdGenerator idGenerator,
      UnsavedDataListener unsavedDataListener, Timer timer) {

    WaveLoader loader = new WaveLoader(isNewWave, waveRef, channel, participant, otherParticipants,
        idGenerator, unsavedDataListener, timer);
    return loader;
  }


  protected WaveLoader(boolean isNewWave, WaveRef waveRef, RemoteViewServiceMultiplexer channel,
      ParticipantId participant,
      Set<ParticipantId> otherParticipants, IdGenerator idGenerator,
      UnsavedDataListener unsavedDataListener, Timer timer) {
    this.signedInuser = participant;
    this.waveRef = waveRef;
    this.isNewWave = isNewWave;
    this.idGenerator = idGenerator;
    this.channel = channel;
    this.otherParticipants = otherParticipants;
    this.unsavedDataListener = unsavedDataListener;
    this.timer = timer;
  }

  protected void init(Command command) {

    waveData = WaveViewDataImpl.create(waveRef.getWaveId());

    if (isNewWave) {

      // Code taken from StageTwoProvider

      // For a new wave, initial state comes from local initialization.
      // getConversations().createRoot().getRootThread().appendBlip();

      // Adding any initial participant to the new wave
      // getConversations().getRoot().addParticipantIds(otherParticipants);

      // getConversations().createRoot().addParticipantIds(otherParticipants);

      // Install diff control before rendering, because logical diff state may
      // need to be adjusted due to arbitrary UI policies.

      getConversations().createRoot().addParticipantIds(otherParticipants);

      getConnector().connect(command);




    } else {

      // For an existing wave, while we're still using the old protocol,
      // rendering must be delayed until the channel is opened, because the
      // initial state snapshots come from the channel.


      // Install diff control before rendering, because logical diff state may
      // need to be adjusted due to arbitrary UI policies.
      getConnector().connect(command);

    }

    isClosed = false;
  }

  public WaveContext getWaveContext() {
    if (isClosed)
      return null;

    if (waveContext == null)
      waveContext = new WaveContext(getWave(), getConversations(), getSupplement(), null);

    return waveContext;
  }

  public void close() {
    if (!isClosed) {
      getConnector().close();
      isClosed = true;
    }
  }

  //
  // Level 1
  //

  protected WaveViewData getWaveData() {
    Preconditions.checkState(waveData != null, "wave not ready");
    return waveData;
  }

  protected ObservableConversationView getConversations() {
    return conversations == null ? conversations = createConversations() : conversations;
  }

  protected ObservableConversationView createConversations() {
    return WaveBasedConversationView.create(getWave(), getIdGenerator());
  }

  protected MuxConnector getConnector() {
    return connector == null ? connector = createConnector() : connector;
  }

  protected MuxConnector createConnector() {
    // LoggerBundle logger = LoggerBundle.NOP_IMPL;

    LoggerBundle loggerOps = new AndroidLoggerBundle("WaveProtocol::ops");
    LoggerBundle loggerDeltas = new AndroidLoggerBundle("WaveProtocol::deltas");
    LoggerBundle loggerCc = new AndroidLoggerBundle("WaveProtocol::CC");
    LoggerBundle loggerView = new AndroidLoggerBundle("WaveProtocol::view");

    LoggerContext loggers = new LoggerContext(loggerOps, loggerDeltas, loggerCc, loggerView);

    IdURIEncoderDecoder uriCodec = new IdURIEncoderDecoder(new ClientPercentEncoderDecoder());
    HashedVersionFactory hashFactory = new HashedVersionZeroFactoryImpl(uriCodec);

    Scheduler scheduler = new FuzzingBackOffScheduler.Builder(getRpcScheduler())
        .setInitialBackOffMs(1000).setMaxBackOffMs(60000).setRandomisationFactor(0.5).build();

    ViewChannelFactory viewFactory = ViewChannelImpl.factory(createWaveViewService(), loggerView);
    UnsavedDataListenerFactory unsyncedListeners = new UnsavedDataListenerFactory() {

      private final UnsavedDataListener listener = unsavedDataListener;

      @Override
      public UnsavedDataListener create(WaveletId waveletId) {
        return listener;
      }

      @Override
      public void destroy(WaveletId waveletId) {
      }
    };

    WaveletId udwId = getIdGenerator().newUserDataWaveletId(getSignedInUser().getAddress());

    ArrayList<String> prefixes = new ArrayList<String>();
    prefixes.add(IdConstants.CONVERSATION_WAVELET_PREFIX);
    prefixes.add(Model.WAVELET_ID_PREFIX);

    final IdFilter filter = IdFilter.of(Collections.singleton(udwId), prefixes);

    WaveletDataImpl.Factory snapshotFactory = WaveletDataImpl.Factory.create(getDocumentRegistry());
    final OperationChannelMultiplexer mux = new OperationChannelMultiplexerImpl(getWave()
        .getWaveId(), viewFactory, snapshotFactory, loggers, unsyncedListeners, scheduler,
        hashFactory);

    final WaveViewImpl<OpBasedWavelet> wave = getWave();

    return new MuxConnector() {
      @Override
      public void connect(Command onOpened) {
        LiveChannelBinder.openAndBind(getWavelets(), wave, getDocumentRegistry(), mux, filter,
            onOpened);
      }

      @Override
      public void close() {
        mux.close();
      }
    };
  }

  //
  // Level 2
  //

  protected ParticipantId getSignedInUser() {
    return signedInuser;
  }

  protected IdGenerator getIdGenerator() {
    return idGenerator; // TODO assess whether create the id generator here
  }

  protected WaveViewImpl<OpBasedWavelet> getWave() {
    return wave == null ? wave = createWave() : wave;
  }

  protected WaveViewImpl<OpBasedWavelet> createWave() {
    WaveViewData snapshot = getWaveData();
    // The operationalizer makes the wavelets function via operation control.
    // The hookup with concurrency-control and remote operation streams occurs
    // later in createUpgrader().
    final WaveletOperationalizer operationalizer = getWavelets();
    WaveletFactory<OpBasedWavelet> waveletFactory = new WaveletFactory<OpBasedWavelet>() {
      @Override
      public OpBasedWavelet create(WaveId waveId, WaveletId id, ParticipantId creator) {
        long now = System.currentTimeMillis();
        ObservableWaveletData data = new WaveletDataImpl(id, creator, now, 0L,
            HashedVersion.unsigned(0), now, waveId, getDocumentRegistry());
        return operationalizer.operationalize(data);
      }
    };
    WaveViewImpl<OpBasedWavelet> wave = WaveViewImpl.create(waveletFactory, snapshot.getWaveId(),
        getIdGenerator(), getSignedInUser(), WaveletConfigurator.ADD_CREATOR);

    // Populate the initial state.
    for (ObservableWaveletData waveletData : snapshot.getWavelets()) {
      wave.addWavelet(operationalizer.operationalize(waveletData));
    }
    return wave;
  }

  protected CollectiveScheduler getRpcScheduler() {
    return rpcScheduler == null ? rpcScheduler = createRpcScheduler() : rpcScheduler;
  }

  protected CollectiveScheduler createRpcScheduler() {



    // Use a scheduler that runs closely-timed tasks at the same time.
    // Adapted Android version from orignal GWT-based
    return new OptimalGroupingScheduler(SchedulerInstance.getLowPriorityTimer());

  }

  protected WaveViewService createWaveViewService() {
    return new RemoteWaveViewService(waveRef.getWaveId(), channel, getDocumentRegistry());
  }


  protected WaveDocuments<CcDataDocumentImpl> getDocumentRegistry() {
    return documentRegistry == null
        ? documentRegistry = createDocumentRegistry() : documentRegistry;
  }

  /**
   * This Document registry separates the pure Wave protocol client code from
   * the former GWT/HTML user interface.
   *
   * In this Android version, there are no referenfes to ContentDocument and
   * LazyContentDocument, actual classes for blip rendering in GWT/HTML.
   *
   * Now all Wave documents are handle as Data Documents.
   *
   * @return WaveDocuments<CcDataDocumentImpl>
   */
  protected WaveDocuments<CcDataDocumentImpl> createDocumentRegistry() {
    IndexedDocumentImpl.performValidation = false;

    // TODO createSchemas() is needed?
    DocumentFactory<?> dataDocFactory =
        ObservablePluggableMutableDocument.createFactory(createSchemas());


    /**
     * Replacement of
     *
     * <pre>
     * DocumentFactory<LazyContentDocument>
     * </pre>
     *
     * By now, no special content documents are managed. No schema constrains
     * are attached to new docs.
     *
     */
    DocumentFactory<CcDataDocumentImpl> fakeBlipDocFactory = new DocumentFactory<CcDataDocumentImpl>() {

      @Override
      public CcDataDocumentImpl create(WaveletId waveletId, String docId, DocInitialization content) {
        return new CcDataDocumentImpl(DocumentSchema.NO_SCHEMA_CONSTRAINTS, content);
      }

    };

    return WaveDocuments.create(fakeBlipDocFactory, dataDocFactory);
  }

  protected WaveletOperationalizer getWavelets() {
    return wavelets == null ? wavelets = createWavelets() : wavelets;
  }

  protected WaveletOperationalizer createWavelets() {
    return WaveletOperationalizer.create(getWaveData().getWaveId(), getSignedInUser());
  }

  //
  // Level 3
  //

  protected SchemaProvider createSchemas() {
    return new ConversationSchemas();
  }

  public final ObservableSupplementedWave getSupplement() {
    return null;
    // Supplement relies on GWT scheduler, so we ignore it
    // anyway
    // return supplement == null ? supplement = createSupplement() : supplement;
  }

  protected ObservableSupplementedWave createSupplement() {
    /*
     * Wavelet udw = getWave().getUserData(); if (udw == null) { udw =
     * getWave().createUserData(); } ObservablePrimitiveSupplement state =
     * WaveletBasedSupplement.create(udw); ObservableSupplementedWave live = new
     * LiveSupplementedWaveImpl(state, getWave(), getSignedInUser(),
     * DefaultFollow.ALWAYS, getConversations()); return
     * LocalSupplementedWaveImpl.create(getWave(), live);
     */
    return null;
  }

}
