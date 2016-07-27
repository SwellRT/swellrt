package org.swellrt.server.box.events;

import junit.framework.TestCase;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EventDispatcherImplTest extends TestCase {


  protected void setUp() throws Exception {
    super.setUp();
  }

  protected EventDispatcherImpl getDispatcher() {

    EventDispatcherImpl dispatcher = new EventDispatcherImpl(new EventQueue() {

      @Override
      public void registerListener(EventQueueListener listener) {

      }

      @Override
      public void registerConfigurator(EventQueueConfigurator configurator) {

      }

      @Override
      public boolean hasEventsFor(String app, String dataType) {
        return false;
      }

      @Override
      public Set<String> getExpressionPaths(String app, String dataType) {
        return null;
      }

      @Override
      public void add(Event event) {

      }
    });

    return dispatcher;
  }

  protected EventDispatcherImpl initialize(EventDispatcherImpl dispatcher)
		  throws UnsupportedEncodingException {


	  InputStreamReader reader =
			  new InputStreamReader(
					  this.getClass().getResourceAsStream("EventDispatcherImplTest_Rules.json"),"UTF-8");


	  Collection<EventRule> rules = EventRule.fromReader(reader);
	  dispatcher.setRules(rules);
	  dispatcher.subscribe(new EventDispatcherTarget() {


		  @Override
		  public String getName() {
			  return "test_dispatcher";
		  }

		  @Override
		  public void dispatch(EventRule rule, Event event, String payload) {

		  }

	  }, "test_dispatcher");


	  return dispatcher;

  }

  /**
   * Test EventDispatcherImpl.initialize(...)
   *
   * @throws UnsupportedEncodingException
   */
  public void testInitialize() throws UnsupportedEncodingException {

    EventDispatcherImpl dispatcher = initialize(getDispatcher());

    Set<EventRuleClass> ruleClasses = dispatcher.getEventRuleClasses();
    assertEquals(3, ruleClasses.size());

    assert (ruleClasses.contains(EventRuleClass.of("APP_01", "DATATYPE_01")));
    assert (ruleClasses.contains(EventRuleClass.of("APP_01", "DATATYPE_02")));
    assert (ruleClasses.contains(EventRuleClass.of("APP_02", "DATATYPE_01")));


  }

  /**
   * Test EventRule.match() and EventDispatcherImpl.onEvent() logic
   *
   * @throws UnsupportedEncodingException
   */
  public void testOnEvent() throws UnsupportedEncodingException {

    final ArrayList<Event> dispatchedEvents = new ArrayList<Event>();

    EventDispatcherImpl dispatcher = initialize(getDispatcher());

    dispatcher.getTargets().put("test_dispatcher", new EventDispatcherTarget() {

      @Override
      public String getName() {
        return "test_dispatcher";
      }

      @Override
      public void dispatch(EventRule rule, Event event, String payload) {
        dispatchedEvents.add(event);
      }

    });

    // EVENT 01

    Map<String, String> eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "pickme");
    eventContextData.put("root.data.fieldtwo", "AAAA");
    eventContextData.put("root.data.fieldthree", "BBBB");

    Event.Builder builder = new Event.Builder();
    builder.app("APP_01").dataType("DATATYPE_01").contextData(eventContextData);

    Event event01 = builder.build(Event.Type.MAP_ENTRY_UPDATED, "root.data.map.value");
    dispatcher.onEvent(event01);

    assertEquals(1, dispatchedEvents.size());
    assertEquals(event01, dispatchedEvents.get(0));

    // EVENT 02

    eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "pickme");
    eventContextData.put("root.info.fieldtwo", "AAAA");
    eventContextData.put("root.info.fieldthree", "BBBB");
    eventContextData.put("root.data.fieldtwo", "CCCC");
    eventContextData.put("root.data.fieldthree", "DDDD");

    builder = new Event.Builder();
    builder.app("APP_01").dataType("DATATYPE_01").contextData(eventContextData);

    Event event02 = builder.build(Event.Type.LIST_ITEM_ADDED, "root.data.list");

    dispatcher.onEvent(event02);

    assertEquals(2, dispatchedEvents.size());
    assertEquals(event02, dispatchedEvents.get(1));


    // EVENT 03

    eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "pickme");
    eventContextData.put("root.info.fieldtwo", "AAAA");
    eventContextData.put("root.info.fieldthree", "BBBB");
    eventContextData.put("root.data.fieldtwo", "CCCC");
    eventContextData.put("root.data.fieldthree", "DDDD");

    builder = new Event.Builder();
    builder.app("APP_01").dataType("DATATYPE_02").contextData(eventContextData);

    Event event03 = builder.build(Event.Type.MAP_ENTRY_UPDATED, "root.data.map.value");
    dispatcher.onEvent(event03);

    assertEquals(3, dispatchedEvents.size());
    assertEquals(event03, dispatchedEvents.get(2));

    // EVENT 04

    eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "pickme");
    eventContextData.put("root.info.fieldtwo", "AAAA");
    eventContextData.put("root.info.fieldthree", "BBBB");
    eventContextData.put("root.data.fieldtwo", "CCCC");
    eventContextData.put("root.data.fieldthree", "DDDD");

    builder = new Event.Builder();
    builder.app("APP_02").dataType("DATATYPE_01").contextData(eventContextData);

    Event event04 = builder.build(Event.Type.LIST_ITEM_ADDED, "root.data.list");
    dispatcher.onEvent(event04);

    assertEquals(4, dispatchedEvents.size());
    assertEquals(event04, dispatchedEvents.get(3));


    // EVENT 05

    eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "pickme");
    eventContextData.put("root.info.fieldtwo", "AAAA");
    eventContextData.put("root.info.fieldthree", "BBBB");
    eventContextData.put("root.data.fieldtwo", "CCCC");
    eventContextData.put("root.data.fieldthree", "DDDD");

    builder = new Event.Builder();
    builder.app("APP_01").dataType("DATATYPE_01").contextData(eventContextData);

    Event event05 = builder.build(Event.Type.LIST_ITEM_ADDED, "root.data.list.3.list");
    dispatcher.onEvent(event05);

    assertEquals(5, dispatchedEvents.size());
    assertEquals(event05, dispatchedEvents.get(4));


    int total_dispatched_events = 5;

    // EVENT 01 - Conditions doesn't match

    eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "NO");
    eventContextData.put("root.info.fieldtwo", "AAAA");
    eventContextData.put("root.info.fieldthree", "BBBB");
    eventContextData.put("root.data.fieldtwo", "CCCC");
    eventContextData.put("root.data.fieldthree", "DDDD");

    builder = new Event.Builder();
    builder.app("APP_02").dataType("DATATYPE_01").contextData(eventContextData);

    Event event01bad = builder.build(Event.Type.LIST_ITEM_ADDED, "root.data.list");
    dispatcher.onEvent(event01bad);

    assertEquals(total_dispatched_events, dispatchedEvents.size());

    // EVENT 02- App doesn't match

    eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "NO");
    eventContextData.put("root.info.fieldtwo", "AAAA");
    eventContextData.put("root.info.fieldthree", "BBBB");
    eventContextData.put("root.data.fieldtwo", "CCCC");
    eventContextData.put("root.data.fieldthree", "DDDD");

    builder = new Event.Builder();
    builder.app("APP_XX").dataType("DATATYPE_01").contextData(eventContextData);

    Event event02bad = builder.build(Event.Type.LIST_ITEM_ADDED, "root.data.list");
    dispatcher.onEvent(event02bad);

    assertEquals(total_dispatched_events, dispatchedEvents.size());

    // EVENT 03- DataType doesn't match

    eventContextData = new HashMap<String, String>();
    eventContextData.put("root.condition_one", "pickme");
    eventContextData.put("root.condition_two", "NO");
    eventContextData.put("root.info.fieldtwo", "AAAA");
    eventContextData.put("root.info.fieldthree", "BBBB");
    eventContextData.put("root.data.fieldtwo", "CCCC");
    eventContextData.put("root.data.fieldthree", "DDDD");

    builder = new Event.Builder();
    builder.app("APP_01").dataType("DATATYPE_XX").contextData(eventContextData);

    Event event03bad = builder.build(Event.Type.LIST_ITEM_ADDED, "root.data.list");
    dispatcher.onEvent(event03bad);

    assertEquals(total_dispatched_events, dispatchedEvents.size());

  }

}
