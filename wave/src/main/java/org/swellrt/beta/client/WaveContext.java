package org.swellrt.beta.client;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.swellrt.beta.client.js.Console;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SStatusEvent;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.waveprotocol.wave.client.editor.content.DocContributionsFetcher;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.concurrencycontrol.common.TurbulenceListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gwt.user.client.Command;

/**
 * A class wrapping Wave components to be managed by the {@ServiceContext}.
 * Handles Wave life cycle and captures Channel Exceptions.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveContext implements UnsavedDataListener, TurbulenceListener, WaveStatus {

  private static final int INACTIVE = 0;
  private static final int ACTIVE = 1;
  private static final int ERROR = 2;

  private int state = INACTIVE;

  private final WaveId waveId;
  private final String waveDomain;
  private final ParticipantId participant;
  private final ServiceStatus serviceStatus;

  private WaveLoader loader;
  private SettableFuture<SObjectRemote> sobjectFuture;
  private ChannelException lastException;

  private final DocContributionsFetcher contributionsFetcher;

  public WaveContext(WaveId waveId, String waveDomain, ParticipantId participant,
      ServiceStatus serviceStatus, DocContributionsFetcher.Factory contributionsFetcherFactory) {
    super();
    this.waveId = waveId;
    this.waveDomain = waveDomain;
    this.participant = participant;
    this.serviceStatus = serviceStatus;
    this.sobjectFuture = SettableFuture.<SObjectRemote> create();
    this.contributionsFetcher = contributionsFetcherFactory.create(waveId);
  }

  public void init(RemoteViewServiceMultiplexer viewServiceMultiplexer, IdGenerator idGenerator) {

    Preconditions.checkArgument(viewServiceMultiplexer != null,
        "Can't init Wave context with a null Remote Service Multiplexer");
    Preconditions.checkArgument(idGenerator != null,
        "Can't init Wave context with a null Id Generator");

    // Clean up listener on the channel multiplexer
    if (loader != null)
      loader.destroy();

    // Create a future for the object
    if (this.sobjectFuture == null || this.sobjectFuture.isDone())
      this.sobjectFuture = SettableFuture.<SObjectRemote> create();

    // Load the wave and bind to the object
    state = ACTIVE;

    loader = new WaveLoader(waveId, viewServiceMultiplexer, idGenerator, waveDomain,
        Collections.<ParticipantId> emptySet(), participant, this, this, this.contributionsFetcher);

    try {

      loader.load(new Command() {

        @Override
        public void execute() {

          try {
            // there was exception during loading process?
            check();
            SObjectRemote sobject = SObjectRemote.inflateFromWave(participant,
                loader.getIdGenerator(), loader.getLocalDomain(), loader.getWave(),
                PlatformBasedFactory.getFactory(loader), WaveContext.this);
            sobjectFuture.set(sobject);

          } catch (SException ex) {
            sobjectFuture.setException(ex);
          }

        }
      });

    } catch (RuntimeException ex) {
      sobjectFuture.setException(ex.getCause());
    }

  }

  public void getSObject(FutureCallback<SObjectRemote> callback) {
    Futures.addCallback(this.sobjectFuture, callback);
  }

  public void close() {
    onClose(false);

    if (loader != null)
      loader.destroy();

    this.state = INACTIVE;
  }

  @Override
  public void onFailure(ChannelException e) {
    this.lastException = e;
    this.state = ERROR;
    Console.log("ChannelException: " + e.toString());
    // If an exception occurs during stage loader (WaveLoader)
    // it will reach here. Check the future so.
    if (!this.sobjectFuture.isDone()) {
      this.sobjectFuture.setException(new SException(e));
    } else {
      try {
        this.sobjectFuture.get().onStatusEvent(new SStatusEvent(
            ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId), new SException(e)));
      } catch (InterruptedException | ExecutionException e1) {
        //
      }
    }
    serviceStatus.raise(ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId), new SException(e));
    close();
  }

  @Override
  public void onUpdate(UnsavedDataInfo unsavedDataInfo) {
    if (this.sobjectFuture.isDone()) {
      try {
        this.sobjectFuture.get()
            .onStatusEvent(new SStatusEvent(ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId),
                unsavedDataInfo.inFlightSize(), unsavedDataInfo.estimateUnacknowledgedSize(),
                unsavedDataInfo.estimateUncommittedSize(), unsavedDataInfo.laskAckVersion(),
                unsavedDataInfo.lastCommitVersion()));
      } catch (InterruptedException | ExecutionException e1) {
        throw new RuntimeException(e1);
      }
    }
  }

  @Override
  public void onClose(boolean everythingCommitted) {
    if (this.sobjectFuture.isDone()) {
      try {
        this.sobjectFuture.get().onStatusEvent(new SStatusEvent(
            ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId), everythingCommitted));
      } catch (InterruptedException | ExecutionException e1) {
        throw new RuntimeException(e1);
      }
    }
  }

  public boolean isError() {
    return this.state == ERROR;
  }

  public boolean isActive() {
    return this.state == ACTIVE;
  }

  @Override
  public void check() throws SException {

    serviceStatus.check();

    if (this.state == INACTIVE || this.state == ERROR) {
      if (lastException != null)
        throw new SException(lastException);
      else
        throw new SException(ResponseCode.UNKNOWN);
    }
  }

}
