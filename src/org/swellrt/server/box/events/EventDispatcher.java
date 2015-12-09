package org.swellrt.server.box.events;

import java.util.Collection;
import java.util.Map;

public interface EventDispatcher extends EventQueueListener, EventQueueConfigurator {

  public void initialize(Map<String, EventDispatcherTarget> targets, Collection<EventRule> rules);

}
