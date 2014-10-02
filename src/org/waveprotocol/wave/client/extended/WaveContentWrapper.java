package org.waveprotocol.wave.client.extended;

import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.box.webclient.search.WaveStore;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.common.util.AsyncHolder.Accessor;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Set;

public class WaveContentWrapper extends Stages {


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
  protected WaveContext wave;
  protected WaveRef waveRef;
  private RemoteViewServiceMultiplexer channel;
  protected IdGenerator idGenerator;
  private ProfileManager profileManager;
  protected WaveStore waveStore;
  protected String localDomain;
  protected Set<ParticipantId> participants;
  protected boolean isNewWave;
  private WaveContentManager.UnsavedDataListenerProxy dataListenerProxy;
  protected ParticipantId loggedInUser;



  protected WaveContentWrapper(WaveRef waveRef, RemoteViewServiceMultiplexer channel,
      IdGenerator idGenerator, WaveStore waveStore, String localDomain,
      Set<ParticipantId> participants, ParticipantId loggedInUser, boolean isNewWave) {
    super();
    this.waveRef = waveRef;
    this.channel = channel;
    this.idGenerator = idGenerator;
    this.waveStore = waveStore;
    this.localDomain = localDomain;
    this.participants = participants;
    this.isNewWave = isNewWave;
    this.loggedInUser = loggedInUser;

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
    return haltIfClosed(new StageTwoProvider(this.one = one, this.waveRef, this.channel,
        this.isNewWave, this.idGenerator, this.dataListenerProxy,
        this.participants));
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
        return WaveContentWrapper.this.localDomain;
      }
    });
  }

  private void onStageThreeLoaded(StageThree x, Accessor<StageThree> whenReady) {
    if (closed) {
      // Stop the loading process.
      return;
    }
    three = x;
    if (this.isNewWave) {
      initNewWave(x);
    } else {
      handleExistingWave(x);
    }
    wave =
        new WaveContext(two.getWave(), two.getConversations(), two.getSupplement(),
 null);
    // this.waveStore.add(wave); // TODO check compatibility with no
    // conversation waves
    install();
    whenReady.use(x);
  }

  private void initNewWave(StageThree three) {
    // Do the new-wave flow.
    ConversationView wave = two.getConversations();

    // Force rendering to finish.
  }

  private void handleExistingWave(StageThree three) {

  }

  /**
   * A hook to install features that are not dependent an a certain stage.
   */
  protected void install() {
    // WindowTitleHandler.install(context.getStore(), context.getWaveFrame());
  }

  public void destroy() {
    if (wave != null) {
      // this.waveStore.remove(wave); // TODO check compatibility with no
      // conversation waves
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


  public WaveContext getWave() {
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

}
