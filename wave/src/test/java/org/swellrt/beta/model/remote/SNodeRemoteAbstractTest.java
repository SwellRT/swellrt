package org.swellrt.beta.model.remote;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SPrimitive;
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
public abstract class SNodeRemoteAbstractTest extends TestCase {
  
  protected IdGenerator idGenerator;
  protected FakeWaveView wave;
  protected SObjectRemote object;
  protected PlatformBasedFactory factory;
  
  protected void setUp() throws Exception {
    
    factory = new FakePlatformBasedFactory();
    idGenerator = FakeIdGenerator.create();
    wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    object = SObjectRemote.inflateFromWave(ParticipantId.ofUnsafe("tom@acme.com"), idGenerator, "example.com", wave, factory, null);
    
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
