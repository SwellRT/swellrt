package x.swellrt.model.mutable;

import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;

import junit.framework.TestCase;
import x.swellrt.model.CMap;
import x.swellrt.model.CPrimitive;
import x.swellrt.model.local.java.LocalCMap;

/**
 * All tests regarding the Mutable hierarchy and Java-based local nodes.
 * 
 * MutableCObject and MutableCMap have a circular dependency so they must be
 * tested together.
 * 
 * @author pablojan@gmail.com
 *
 */
public class MutableCObjectTest extends TestCase {

  private IdGenerator idGenerator;
  private FakeWaveView wave;
  private MutableCObject object;
  
  protected void setUp() throws Exception {
    
    idGenerator = FakeIdGenerator.create();
    wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    object = MutableCObject.ofWave(idGenerator, "example.com", wave);
    
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
  
  protected void populatePrimitiveValues(CMap map) {
    
    map.put("k0", new CPrimitive("A value for k0"));
    map.put("k1", "A value for k1");
    
  }
  
  protected void assertPrimitiveValues(CMap map) {
    
    assertEquals("A value for k0", map.get("k0").asString());
    assertEquals("A value for k1", map.get("k1").asString());
    
  }
  
  /**
   * Put only primitive values in the root map.
   * 
   * Get primitive values with or without map cache 
   * to force deserialization from Wave documents (XML)
   * 
   */
  public void testMapWithPrimitiveValues() {
    
        
    populatePrimitiveValues(object.getRoot());
    assertPrimitiveValues(object.getRoot());
    
    object.getRoot().clearCache();
    
    populatePrimitiveValues(object.getRoot());
    assertPrimitiveValues(object.getRoot());
    
  }
  
  /**
   * Put a nested map in root map. 
   * Put primitive values in inner map. 
   * 
   * Get primitive values with or without map cache to  
   * force deserialization from Wave documents (XML)
   * 
   */
  public void testMapWithNestedMap() {

    
    CMap submap = new LocalCMap();
    
    populatePrimitiveValues(submap);
    
    submap = (CMap) object.getRoot().put("submap", submap);
    
    assertPrimitiveValues(submap);
    
    
  }
  
  
  protected void tearDown() throws Exception {
    super.tearDown();
  }

}
