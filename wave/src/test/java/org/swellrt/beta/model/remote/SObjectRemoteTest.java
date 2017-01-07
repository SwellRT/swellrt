package org.swellrt.beta.model.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SHandler;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.local.SMapLocal;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.waveprotocol.wave.client.common.util.CountdownLatch;
import org.waveprotocol.wave.client.render.ReductionRuleRenderHelperEquivalenceTest;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;

import com.google.gwt.user.client.Command;

import junit.framework.TestCase;


/**
 * Unit tests for the SwellRT data model
 * <p>
 * SObjectRemote and SMapRemote have a circular dependency so they must be
 * tested together.
 * <p>
 * @author pablojan@gmail.com
 *
 */
public class SObjectRemoteTest extends TestCase {

  private IdGenerator idGenerator;
  private FakeWaveView wave;
  private SObjectRemote object;
  
  protected void setUp() throws Exception {
    
    idGenerator = FakeIdGenerator.create();
    wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    object = SObjectRemote.inflateFromWave(idGenerator, "example.com", wave, null);
    
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
  
  protected void populatePrimitiveValues(SMap map) throws SException {
    
    map.put("k0", new SPrimitive("A value for k0"));
    map.put("k1", "A value for k1");
    
  }
  
  protected void assertPrimitiveValues(SMap map) throws SException {
    
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
  public void testMapWithPrimitiveValues() throws SException {
    
        
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
  public void testMapWithNestedMap() throws SException {

    
    SMap submap = new SMapLocal();
    
    populatePrimitiveValues(submap);
    
    object.put("submap", submap);
    
    assertPrimitiveValues((SMap) object.get("submap"));
    
    
  }
  
  /**
   * Create two nested local maps<br>
   * Add them to a live object (so create remote maps)<br>
   * Check primitive values are retrieved from remote maps<br>
   * Make changes directly in remote maps<br>
   * 
   * @throws SException
   */
  public void testMapWithNestedLocalMap() throws SException {
    
    // Create maps    
    SMap mapA = new SMapLocal();
    populatePrimitiveValues(mapA);
    
    SMap mapB = new SMapLocal();
    populatePrimitiveValues(mapB);
    
    mapA.put("mapB", mapB);    
    object.put("mapA", mapA);
    
    
    // Clear cache and check retrieving values
    object.clearCache();
    
    SMap remoteMapA = (SMap) object.get("mapA");        
    assertPrimitiveValues(remoteMapA);
    SMap remoteMapB = (SMap) remoteMapA.get("mapB");  
    assertPrimitiveValues(remoteMapB);
    
    // Make changes, no exceptions must be thrown
    remoteMapA.put("kA1", "Some value kA1");
    remoteMapB.put("kB1", "Some value kB1");
    
    object.clearCache();
    
    assertEquals("Some value kA1", (String) remoteMapA.get("kA1"));
    assertEquals("Some value kB1", (String) remoteMapB.get("kB1"));
    
  }
  
  /**
   * Test map events generated by local changes.
   * 
   * @throws SException
   * @throws InterruptedException 
   */
  public void testMapEvents() throws SException, InterruptedException {
   
    
    List<SEvent> recvEvents = new ArrayList<SEvent>();
    
    SHandler eventHandler = new SHandler() {

      @Override
      public boolean onEvent(SEvent e) { 
        recvEvents.add(e);
        synchronized (this) {
          if (recvEvents.size() == 3)
            notifyAll();
        }
        
        return false;
      }

    };
    
    SMap map = new SMapLocal();
    populatePrimitiveValues(map);
    object.put("map", map);
    SMapRemote remoteMap =(SMapRemote) object.get("map");
    remoteMap.addHandler(eventHandler);
    
    remoteMap.remove("k1");
    remoteMap.put("k2" , "This is new value");
    remoteMap.put("k0" , "This is updated value");
    
    synchronized (eventHandler) {
      eventHandler.wait(1000);
    }
    
    // Check whether mutations were properly done
    assertEquals(2, remoteMap.size());
    assertEquals(null, remoteMap.get("k1"));
    assertEquals("This is new value", (String) remoteMap.get("k2"));
    assertEquals("This is updated value", (String) remoteMap.get("k0"));
    
    // Check events    
    assertEquals(3, recvEvents.size());
    
    assertEquals(SEvent.REMOVED_VALUE, recvEvents.get(0).getType());
    assertEquals("k1", recvEvents.get(0).getTargetKey());
    assertEquals("A value for k1", (String) ((SPrimitive) recvEvents.get(0).getValue()).getObject());
    
    assertEquals(SEvent.ADDED_VALUE, recvEvents.get(1).getType());
    assertEquals("k2", recvEvents.get(1).getTargetKey());
    assertEquals("This is new value", (String) ((SPrimitive) recvEvents.get(1).getValue()).getObject());
    
    assertEquals(SEvent.UPDATED_VALUE, recvEvents.get(2).getType());
    assertEquals("k0", recvEvents.get(2).getTargetKey());
    assertEquals("This is updated value", (String) ((SPrimitive) recvEvents.get(2).getValue()).getObject());
  }
  
  
  /**
   * Test whether events generated by map changes are properly
   * propagated upwards within nested maps.
   * @throws SException 
   * @throws InterruptedException 
   */
  public void testMapEventsPropagation() throws SException, InterruptedException {

    List<SEvent> capturedEventsRoot = new ArrayList<SEvent>();
    List<SEvent> capturedEventsMapA = new ArrayList<SEvent>();
    List<SEvent> capturedEventsMapB = new ArrayList<SEvent>();
    
    CountdownLatch cd = CountdownLatch.create(5, new  Command() {
      
      @Override
      public void execute() {
        
        // Case 1) Generate event in C 
        // captured by handlerB but not in handlerA and handlerRoot
        SEvent e = capturedEventsMapB.get(0);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForC", (String) ((SPrimitive) e.getValue()).getObject());
        
        // Case 2) Generate event in B
        // captured by handlerB but not in handlerA and handlerRoot
        e = capturedEventsMapB.get(1);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForB", (String) ((SPrimitive) e.getValue()).getObject());
        
        // Case 3) Generate event in A
        // captured by handlerA and rootHandler
        e = capturedEventsMapA.get(0);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForA", (String) ((SPrimitive) e.getValue()).getObject()); 
        assertEquals(1, capturedEventsMapA.size()); // Assert case 1 and case 2, "but" parts
        
        e = capturedEventsRoot.get(0);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForA", (String) ((SPrimitive) e.getValue()).getObject());  
        assertEquals(1, capturedEventsRoot.size()); // Assert case 1 and case 2, "but" parts
        
      }
    });
    

    
    SHandler handlerRoot = new SHandler() {

      @Override
      public boolean onEvent(SEvent e) { 
        capturedEventsRoot.add(e);
        // System.out.println("handlerRoot: "+e.toString());
        cd.tick();
        // for root handler this has not actual effect
        return true; 
      }

    };
    
    SHandler handlerMapA = new SHandler() {

      @Override
      public boolean onEvent(SEvent e) { 
        capturedEventsMapA.add(e);
        // System.out.println("handlerMapA: "+e.toString());
        cd.tick();
        // this handler will let
        // events to flow upwards
        return true; 
      }

    };
    
    SHandler handlerMapB = new SHandler() {

      @Override
      public boolean onEvent(SEvent e) { 
        capturedEventsMapB.add(e);
        // System.out.println("handlerMapB: "+e.toString());
        cd.tick();
        // this handler won't let
        // events to flow upwards
        return false; 
      }

    };    
    
    // Create test data and bind event listeners
    
    // SMap map = new SMapLocal();
    //populatePrimitiveValues(map);
    
    //
    // root map <- hanlderRoot
    //   |
    //   -- mapA  <- handlerA (allow propagation)
    //       |
    //       -- mapB <- handlerB (stop propagation)
    //           |
    //           -- mapC 
    //

    SMapRemote remoteMapA = (SMapRemote) object.put("mapA", new SMapLocal()).get("mapA");
    SMapRemote remoteMapB = (SMapRemote) remoteMapA.put("mapB", new SMapLocal()).get("mapB");
    SMapRemote remoteMapC = (SMapRemote) remoteMapB.put("mapC", new SMapLocal()).get("mapC");
    
    // Set handlers here to ignore events for initialization fields
    remoteMapA.addHandler(handlerMapA);
    remoteMapB.addHandler(handlerMapB);
    object.addHandler(handlerRoot);
    
    // Case 1) Generate event in C 
    // captured by handlerB but not in handlerA and handlerRoot
    remoteMapC.put("keyForC", "valueForC");
    
    // Case 2) Generate event in B
    // captured by handlerB but not in handlerA and handlerRoot
    remoteMapB.put("keyForB", "valueForB");
    
    // Case 3) Generate event in A
    // captured by handlerA and rootHandler
    remoteMapA.put("keyForA", "valueForA");
    
    cd.tick();
    
  }
  
  
  protected void tearDown() throws Exception {
    super.tearDown();
  }

}