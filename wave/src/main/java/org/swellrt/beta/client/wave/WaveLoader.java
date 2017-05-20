package org.swellrt.beta.client.wave;

import java.util.Set;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.common.util.AsyncHolder.Accessor;
import org.waveprotocol.wave.client.editor.content.DocContributionsFetcher;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.concurrencycontrol.common.TurbulenceListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;
import org.waveprotocol.wave.model.waveref.WaveRef;

/**
 * Wave Loader orchestrate the process of instantiate a Wavelet with its Blip/Documents,
 * and connect it to the server.
 * <p>
 * It uses the original stage-based load process optimized for browsers and the
 * conversational model. TODO consider to simplify the staged loader.
 * <p>
 * Important! This class and all Stage classes are platform dependent.
 * <p>
 * Use the method {@link Stages#load} to launch the load process.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveLoader extends Stages {


  private final static AsyncHolder<Object> HALT = new AsyncHolder<Object>() {
    @Override
    public void call(Accessor<Object> accessor) {
      // Never ready, so never notify the accessor.
    }
  };

  protected boolean closed;
  private StageOne one;
  private StageTwo two;
  private StageThree three;
  protected ObservableWaveView wave;
  protected WaveId waveId;
  private RemoteViewServiceMultiplexer channel;
  protected IdGenerator idGenerator;
  private ProfileManager profileManager;
  protected String localDomain;
  protected Set<ParticipantId> participants;
  protected boolean isNewWave;
  private UnsavedDataListener dataListener;
  protected ParticipantId loggedInUser;
  private TurbulenceListener turbulenceListener;
  private DocContributionsFetcher contribFetcher;


  public WaveLoader(WaveId waveId, RemoteViewServiceMultiplexer channel,
      IdGenerator idGenerator, String localDomain,
 Set<ParticipantId> participants, ParticipantId loggedInUser,
      UnsavedDataListener dataListener, TurbulenceListener turbulenceListener, DocContributionsFetcher contribFetcher) {
    super();
    this.waveId = waveId;
    this.channel = channel;
    this.idGenerator = idGenerator;
    this.localDomain = localDomain;
    this.participants = participants;
    this.loggedInUser = loggedInUser;
    this.dataListener = dataListener;
    this.turbulenceListener = turbulenceListener;
    this.contribFetcher = contribFetcher;

  }


  //
  // Stages staff
  //

  @Override
  protected AsyncHolder<StageZero> createStageZeroLoader() {
    return haltIfClosed(super.createStageZeroLoader());
  }

  @Override
  protected AsyncHolder<StageOne> createStageOneLoader(StageZero zero) {
    return haltIfClosed(new StageOne.DefaultProvider(zero) {});
  }

  @Override
  protected AsyncHolder<StageTwo> createStageTwoLoader(StageOne one) {
    return haltIfClosed(new StageTwoProvider(this.one = one, WaveRef.of(this.waveId), this.channel,
        this.isNewWave, this.idGenerator, this.dataListener,
        this.participants, this.loggedInUser, turbulenceListener, this.contribFetcher));
  }

  @Override
  protected AsyncHolder<StageThree> createStageThreeLoader(final StageTwo two) {
    return haltIfClosed(new StageThree.DefaultProvider(this.two = two) {
      @Override
      protected void create(final Accessor<StageThree> whenReady) {
        // Prepend an init wave flow onto the stage continuation.
        super.create(new Accessor<StageThree>() {
          @Override
          public void use(StageThree x) {
            onStageThreeLoaded(x, whenReady);
          }
        });
      }

      @Override
      protected String getLocalDomain() {
        return WaveLoader.this.localDomain;
      }
    });
  }

  private void onStageThreeLoaded(StageThree x, Accessor<StageThree> whenReady) {
    if (closed) {
      // Stop the loading process.
      return;
    }
    three = x;
    wave = two.getWave();
    // Add into some Wave store?
    install();
    whenReady.use(x);
  }


  /**
   * A hook to install features that are not dependent an a certain stage.
   */
  protected void install() {
    // WindowTitleHandler.install(context.getStore(), context.getWaveFrame());
  }

  /**
   * Dispose resources of this Wave, also remove listeners
   * from underlying {@OperationChannelMultiplexer}
   */
  public void destroy() {
    if (wave != null) {
      // Remove from some wave store
      wave = null;
    }
    if (three != null) {
      // e.g. three.getEditActions().stopEditing();
      three = null;
    }
    if (two != null) {
      two.getConnector().close();
      two = null;
    }
    if (one != null) {
      // e.g. one.getWavePanel().destroy();
      one = null;
    }
    closed = true;
  }


  /**
   * @return a halting provider if this stage is closed. Otherwise, returns the
   *         given provider.
   */
  @SuppressWarnings("unchecked")
  // HALT is safe as a holder for any type
  private <T> AsyncHolder<T> haltIfClosed(AsyncHolder<T> provider) {
    return closed ? (AsyncHolder<T>) HALT : provider;
  }


  public ObservableWaveView getWave() {
    return wave;
  }


  public String getLocalDomain() {
    return localDomain;
  }


  public boolean isNewWave() {
    return isNewWave;
  }


  public ParticipantId getLoggedInUser() {
    return loggedInUser;
  }

  public IdGenerator getIdGenerator() {
    return idGenerator;

  }

  public SWaveDocuments<? extends InteractiveDocument> getDocumentRegistry() {
    return two.getDocumentRegistry();
  }

  public boolean isClosed() {
    return closed;
  }
}
