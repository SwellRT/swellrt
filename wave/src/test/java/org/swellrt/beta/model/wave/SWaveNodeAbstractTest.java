package org.swellrt.beta.model.wave;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.testing.FakePlatformBasedFactory;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ParticipantId;

import junit.framework.TestCase;

/**
 * Base class for tests regarding SNodeRemote hierarchy
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public abstract class SWaveNodeAbstractTest extends TestCase {

  protected IdGenerator idGenerator;
  protected FakeWaveView wave;
  protected SWaveObject object;
  protected PlatformBasedFactory factory;

  protected void setUp() throws Exception {

    factory = new FakePlatformBasedFactory();
    idGenerator = FakeIdGenerator.create();
    wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();

    SWaveNodeManager nodeManager = SWaveNodeManager.of(ParticipantId.ofUnsafe("tom@acme.com"),
        idGenerator, "example.com", wave, null, factory);
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
