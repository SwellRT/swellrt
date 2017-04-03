package org.swellrt.beta.model.remote;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SHandler;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SPrimitive;

public class SListRemoteTest extends SNodeRemoteAbstractTest {

 
  
  @SuppressWarnings("rawtypes")
  public void testAddOperation() throws SException {
    
    
    SList localList = SList.create();
    localList.add("hello world");
    localList.add(12345);
    localList.add(false);
    
    SMap localMap = SMap.create();
    localMap.put("key0", "value0");
    localMap.put("key1", "value1");
    
    localList.add(localMap);       
    
    object.put("list", localList);    
    SListRemote remoteList = (SListRemote) object.get("list");
    
    assertNotNull(remoteList);
    assertEquals(4, remoteList.size());
    assertEquals("hello world", (String) remoteList.get(0));
    assertEquals(12345, ((Integer) remoteList.get(1)).intValue());
    assertEquals(false, ((Boolean) remoteList.get(2)).booleanValue());
    assertTrue(remoteList.get(3) instanceof SMap);
    
    SMapRemote remoteMap = (SMapRemote) remoteList.get(3);
    assertEquals("value0", remoteMap.get("key0"));
    assertEquals("value1", remoteMap.get("key1"));
    
    assertEquals(3, remoteList.indexOf(remoteMap));
    
    remoteList.values().forEach(new Consumer<SNodeRemote>() {

      int counter = 0;
      
      @Override
      public void accept(SNodeRemote t) {
        
        switch(counter++) {
          
        case 0:
          assertEquals("hello world", SPrimitive.asString(t));
          break;

        case 1:
          assertEquals((int) 12345, (int) SPrimitive.asInt(t));
          break;          

        case 2:
          assertEquals((boolean) false, (boolean) SPrimitive.asBoolean(t));
          break;  
          
        case 3:
          assertTrue(t instanceof SMap);
          break;  
                    
          
        }
        
      }
      
    });
    
  }
  
  
  @SuppressWarnings("rawtypes")
  public void testRemoveOperation() throws SException {
        
    SList localList = SList.create();
    localList.add("hello world");
    localList.add(12345);
    localList.add(false);
    
    SMap localMap = SMap.create();
    localMap.put("key0", "value0");
    localMap.put("key1", "value1");
    
    localList.add(localMap);       
    
    object.put("list", localList);    
    SList remoteList = (SList) object.get("list");
    assertEquals(4, remoteList.size());
    
    // Remove first
    remoteList.remove(0);     
    assertEquals(3, remoteList.size());
    assertEquals(12345, ((Integer) remoteList.get(0)).intValue());
    
    // Remove last
    remoteList.remove(2);
    assertEquals(2, remoteList.size());
    assertEquals(12345, ((Integer) remoteList.get(0)).intValue());
    assertEquals(false, ((Boolean) remoteList.get(1)).booleanValue());

  }
  
  @SuppressWarnings("rawtypes")
  public void testClearOperation() throws SException {
    
    SList localList = SList.create();
    localList.add("hello world");
    localList.add(12345);
    localList.add(false);
    
    SMap localMap = SMap.create();
    localMap.put("key0", "value0");
    localMap.put("key1", "value1");
    
    localList.add(localMap);       
    
    object.put("list", localList);    
    SList remoteList = (SList) object.get("list");
    remoteList.clear();
    assertEquals(0, remoteList.size());
    assertTrue(remoteList.isEmpty());
    
  }
  
  @SuppressWarnings("rawtypes")
  public void testEvents()  throws SException, InterruptedException {
    
    SList localList = SList.create();
    localList.add("hello world");
    localList.add(12345);
    localList.add(false);
    
    SMap localMap = SMap.create();
    localMap.put("key0", "value0");
    localMap.put("key1", "value1");
    
    localList.add(localMap);       
    
    object.put("list", localList);
    SListRemote remoteList = (SListRemote) object.get("list");
    
    
    
    final ArrayList<SEvent> recvEvents = new ArrayList<SEvent>();
    SHandler eventHandler = new SHandler() {
      
      @Override
      public boolean exec(SEvent e) {
        recvEvents.add(e);
        synchronized (this) {
          if (recvEvents.size() == 4)
            notifyAll();
        }
        
        return false;        
      }
    };
    remoteList.listen(eventHandler);
    
    localMap = SMap.create();
    localMap.put("key0", "value0");
    localMap.put("key1", "value1");
    remoteList.add(localMap);
    
    remoteList.remove(1).remove(1);    
    remoteList.add("some words");
    
    synchronized (eventHandler) {
      eventHandler.wait(1000);
    }
    
    assertEquals(4, remoteList.size());
    assertEquals(4, recvEvents.size()); 
    
    assertEquals(SEvent.ADDED_VALUE, recvEvents.get(0).getType());
    assertTrue(recvEvents.get(0).getTarget() instanceof SList);
    assertTrue(recvEvents.get(0).getValue() instanceof SMap);
    
    assertEquals(SEvent.REMOVED_VALUE, recvEvents.get(1).getType());
    assertTrue(recvEvents.get(1).getTarget() instanceof SList);    
    assertEquals((int) 12345, (int) SPrimitive.asInt(recvEvents.get(1).getValue()));
    
    assertEquals(SEvent.REMOVED_VALUE, recvEvents.get(2).getType());
    assertTrue(recvEvents.get(2).getTarget() instanceof SList);
    assertEquals((boolean) false, (boolean) SPrimitive.asBoolean(recvEvents.get(2).getValue()));
    
    assertEquals(SEvent.ADDED_VALUE, recvEvents.get(3).getType());
    assertTrue(recvEvents.get(3).getTarget() instanceof SList);    
    assertEquals("some words", SPrimitive.asString(recvEvents.get(3).getValue()));    
    
  }
  
  
}
