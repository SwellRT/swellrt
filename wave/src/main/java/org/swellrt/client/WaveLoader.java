package org.swellrt.client;

import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.client.common.util.AsyncHolder.Accessor;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Set;

/**
 * The Wave Loader encapsulates the process of loading a Wave and Wavelets from
 * the server and build the in-memory structure.
 * 
 * It uses the original stage-based load process optimized for browsers and the
 * conversational model. TODO consider to simplify the staged loader.
 * 
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
  protected WaveContext wave;
  protected WaveRef waveRef;
  private RemoteViewServiceMultiplexer channel;
  protected IdGenerator idGenerator;
  private ProfileManager profileManager;
  protected String localDomain;
  protected Set<ParticipantId> participants;
  protected boolean isNewWave;
  private UnsavedDataListener dataListener;
  protected ParticipantId loggedInUser;


  public WaveLoader(WaveRef waveRef, RemoteViewServiceMultiplexer channel,
      IdGenerator idGenerator, String localDomain,
 Set<ParticipantId> participants, ParticipantId loggedInUser,
      UnsavedDataListener dataListener) {
    super();
    this.waveRef = waveRef;
    this.channel = channel;
    this.idGenerator = idGenerator;
    this.localDomain = localDomain;
    this.participants = participants;
    this.loggedInUser = loggedInUser;
    this.dataListener = dataListener;

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
        this.isNewWave, this.idGenerator, this.dataListener,
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
    wave =
        new WaveContext(two.getWave(), two.getConversations(), two.getSupplement(),
 null);
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

  public IdGenerator getIdGenerator() {
    return idGenerator;

  }

  public WaveDocuments<? extends InteractiveDocument> getDocumentRegistry() {
    return two.getDocumentRegistry();
  }

  public boolean isClosed() {
    return closed;
  }
}
