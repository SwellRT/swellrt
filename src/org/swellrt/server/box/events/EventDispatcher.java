package org.swellrt.server.box.events;

import java.util.Collection;
import java.util.Map;

/**
 * The dispatcher is the central piece of the events subsystem. It consumes
 * events from the queue, checks events against rules and dispatch them if
 * conditions are true.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public interface EventDispatcher extends EventQueueListener, EventQueueConfigurator {

  public void initialize(Map<String, EventDispatcherTarget> targets, Collection<EventRule> rules);

}
