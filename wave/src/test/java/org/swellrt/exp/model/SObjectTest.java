package org.swellrt.exp.model;

import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.local.SMapLocal;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;

import junit.framework.TestCase;


/**
 * All tests regarding the SObject datam odel
 * <p>
 * SObjectRemote and SMapRemote have a circular dependency so they must be
 * tested together.
 * <p>
 * @author pablojan@gmail.com
 *
 */
public class SObjectTest extends TestCase {

  private IdGenerator idGenerator;
  private FakeWaveView wave;
  private SObjectRemote object;
  
  protected void setUp() throws Exception {
    
    idGenerator = FakeIdGenerator.create();
    wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    object = SObjectRemote.inflateFromWave(idGenerator, "example.com", wave);
    
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
  
  protected void populatePrimitiveValues(SMap map) throws IllegalCastException {
    
    map.put("k0", new SPrimitive("A value for k0"));
    map.put("k1", "A value for k1");
    
  }
  
  protected void assertPrimitiveValues(SMap map) {
    
    assertEquals("A value for k0", (String) map.get("k0"));
    assertEquals("A value for k1", (String) map.get("k1"));
    
  }
  
  /**
   * Put only primitive values in the root map.
   * 
   * Get primitive values with or without map cache 
   * to force deserialization from Wave documents (XML)
   * @throws IllegalCastException 
   * 
   */
  public void testMapWithPrimitiveValues() throws IllegalCastException {
    
        
    populatePrimitiveValues(object);
    assertPrimitiveValues(object);
    
    object.clearCache();
    
    populatePrimitiveValues(object);
    assertPrimitiveValues(object);
    
  }
  
  /**
   * Put a nested map in root map. 
   * Put primitive values in inner map. 
   * 
   * Get primitive values with or without map cache to  
   * force deserialization from Wave documents (XML)
   * @throws IllegalCastException 
   * 
   */
  public void testMapWithNestedMap() throws IllegalCastException {

    
    SMap submap = new SMapLocal();
    
    populatePrimitiveValues(submap);
    
    object.put("submap", submap);
    
    assertPrimitiveValues((SMap) object.get("submap"));
    
    
  }
  
  
  protected void tearDown() throws Exception {
    super.tearDown();
  }

}
