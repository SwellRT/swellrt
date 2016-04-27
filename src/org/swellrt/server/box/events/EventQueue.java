package org.swellrt.server.box.events;

import java.util.Set;

public interface EventQueue {

  public void add(Event event);

  public boolean hasEventsFor(String app, String dataType);

  public Set<String> getExpressionPaths(String app, String dataType);

  public void registerListener(EventQueueListener listener);

  public void registerConfigurator(EventQueueConfigurator configurator);

}
