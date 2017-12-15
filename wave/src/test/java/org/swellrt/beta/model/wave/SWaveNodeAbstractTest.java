package org.swellrt.beta.model.wave;

import org.swellrt.beta.model.ModelFactory;
import org.swellrt.beta.model.java.JavaModelFactory;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ParticipantId;

import junit.framework.TestCase;

/**
 * Base class for tests regarding SNodeRemote hierarchy
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public abstract class SWaveNodeAbstractTest extends TestCase {

  private static ModelFactory modelFactory = new JavaModelFactory();

  private static SWaveNodeManager.NodeFactory nodeFactory = new SWaveNodeManager.NodeFactory() {

    @Override
    public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
        Blip blip) {

      return modelFactory.createWaveText(nodeManager, substrateId, blip, null);
    }

  };

  protected IdGenerator idGenerator;
  protected FakeWaveView wave;
  protected SWaveObject object;

  protected void setUp() throws Exception {

    idGenerator = FakeIdGenerator.create();
    wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();

    SWaveNodeManager nodeManager = SWaveNodeManager.of(ParticipantId.ofUnsafe("tom@acme.com"),
        idGenerator, "example.com", wave, null, nodeFactory);
    object = SWaveObject.materialize(nodeManager);

    // A different way to create a fake wave
    /*
    SchemaProvider SCHEMA_PROVIDER = new SchemaProvider() {

      @Override
      public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
        if (Model.isModelWaveletId(waveletId)) {
          if (TextType.isTextBlipId(documentId)) {
            return ModelSchemas.TEXT_DOCUMENT_SCHEMA;
          }
          // TODO Add more schemas for List, Root, Map...
        }
        return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
      }
    };

    IdGenerator ID_GENERATOR = FakeIdGenerator.create();

    String DOMAIN = "example.com";

    FakeWaveView wave = FakeWaveView.builder(SCHEMA_PROVIDER).with(ID_GENERATOR).build();


    MutableCObject object = MutableCObject.ofWave(DOMAIN, wave);
    */
  }


}
