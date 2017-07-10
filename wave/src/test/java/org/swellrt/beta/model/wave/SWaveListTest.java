package org.swellrt.beta.model.wave;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SHandlerFunc;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.wave.SWaveList;
import org.swellrt.beta.model.wave.SWaveMap;
import org.swellrt.beta.model.wave.SWaveNode;

public class SWaveListTest extends SWaveNodeAbstractTest {



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
    SWaveList remoteList = (SWaveList) object.pick("list");

    assertNotNull(remoteList);
    assertEquals(4, remoteList.size());
    assertEquals("hello world", SPrimitive.asString(remoteList.pick(0)));
    assertEquals(12345, SPrimitive.asInt(remoteList.pick(1)).intValue());
    assertEquals(false, SPrimitive.asBoolean(remoteList.pick(2)).booleanValue());
    assertTrue(remoteList.pick(3) instanceof SMap);

    SWaveMap remoteMap = (SWaveMap) remoteList.pick(3);
    assertEquals("value0", remoteMap.get("key0"));
    assertEquals("value1", remoteMap.get("key1"));

    assertEquals(3, remoteList.indexOf(remoteMap));

    remoteList.values().forEach(new Consumer<SWaveNode>() {

      int counter = 0;

      @Override
      public void accept(SWaveNode t) {

        switch(counter++) {

        case 0:
          assertEquals("hello world", SPrimitive.asString(t));
          break;

        case 1:
          assertEquals(12345, (int) SPrimitive.asInt(t));
          break;

        case 2:
          assertEquals(false, (boolean) SPrimitive.asBoolean(t));
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
    SList remoteList = (SList) object.pick("list");
    assertEquals(4, remoteList.size());

    // Remove first
    remoteList.remove(0);
    assertEquals(3, remoteList.size());
    assertEquals(12345, SPrimitive.asInt(remoteList.pick(0)).intValue());

    // Remove last
    remoteList.remove(2);
    assertEquals(2, remoteList.size());
    assertEquals(12345, SPrimitive.asInt(remoteList.pick(0)).intValue());
    assertEquals(false, SPrimitive.asBoolean(remoteList.pick(1)).booleanValue());

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
    SList remoteList = (SList) object.pick("list");
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
    SWaveList remoteList = (SWaveList) object.pick("list");



    final ArrayList<SEvent> recvEvents = new ArrayList<SEvent>();
    SHandlerFunc eventHandler = new SHandlerFunc() {

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
    remoteList.addListener(eventHandler, null);

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
    assertEquals(12345, (int) SPrimitive.asInt(recvEvents.get(1).getValue()));

    assertEquals(SEvent.REMOVED_VALUE, recvEvents.get(2).getType());
    assertTrue(recvEvents.get(2).getTarget() instanceof SList);
    assertEquals(false, (boolean) SPrimitive.asBoolean(recvEvents.get(2).getValue()));

    assertEquals(SEvent.ADDED_VALUE, recvEvents.get(3).getType());
    assertTrue(recvEvents.get(3).getTarget() instanceof SList);
    assertEquals("some words", SPrimitive.asString(recvEvents.get(3).getValue()));

  }


}
