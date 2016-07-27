package org.swellrt.server.box.objectapi;

import java.util.List;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.operation.CapturingOperationSink;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import com.google.common.collect.Lists;

/**
 * A wavelet wrapper to record the operations (deltas) performed to it.
 * This is a simplified version of {@link RobotWaveletData}
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class OpRecorderWavelet {

	
	  private final ReadableWaveletData snapshot;

	  private final HashedVersion snapshotVersion;

	  private final ParticipantId opAuthor;
	  
	  private OpBasedWavelet wavelet;
	  
	  private CapturingOperationSink<WaveletOperation> outputSink;
	  
	/**
	 * Constructs a new {@link OpRecorderWavelet}. The given
	 * {@link ReadableWaveletData} will be copied by the constructor.
	 *
	 * @param snapshot the base {@link ReadableWaveletData} from which
	 *        {@link OpBasedWavelet} are created.
	 * @param committedVersion the committed version of the given snapshot, used
	 *        to generate deltas.
	 * @param opAuthor the participant who will perform ops.
	 */
	public OpRecorderWavelet(ReadableWaveletData snapshot, HashedVersion committedVersion, ParticipantId opAuthor) {
		this.snapshot = WaveletDataUtil.copyWavelet(snapshot);
		this.snapshotVersion = committedVersion;
		this.opAuthor = opAuthor;
	}
	
	
	/**
	 * Returns an {@link OpBasedWavelet} on which operations can be performed. The
	 * operations are collected and can be returned in the
	 * form of deltas by calling getDeltas().
	 *
	 */
	public OpBasedWavelet getWavelet() {

		if (wavelet != null)
			return wavelet;
		
		DocumentFactory<?> docFactory =
				ObservablePluggableMutableDocument.createFactory(SchemaCollection.empty());

		ObservableWaveletData waveletData =
				WaveletDataImpl.Factory.create(docFactory).create(snapshot);

		SilentOperationSink<WaveletOperation> executor =
				SilentOperationSink.Executor.<WaveletOperation, WaveletData>build(waveletData);
		// Build sink that gathers these ops
		outputSink =
				new CapturingOperationSink<WaveletOperation>();

		BasicWaveletOperationContextFactory contextFactory =
				new BasicWaveletOperationContextFactory(opAuthor);
		wavelet =
				new OpBasedWavelet(waveletData.getWaveId(), waveletData, contextFactory,
						ParticipationHelper.DEFAULT, executor, outputSink);

		return wavelet;
	}

	/**
	 * Returns a list of deltas for all the operations performed on this wavelet.
	 * The deltas apply to the version given during construction of the
	 * {@link OpRecorderWavelet}.
	 */
	public List<WaveletDelta> getDeltas() {
		List<WaveletDelta> deltas = Lists.newArrayList();
		
		WaveletDelta delta = new WaveletDelta(opAuthor, snapshotVersion, outputSink.getOps());
		deltas.add(delta);

		return deltas;
	}
}
