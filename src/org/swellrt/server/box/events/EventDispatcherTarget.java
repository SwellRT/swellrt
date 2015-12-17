package org.swellrt.server.box.events;


public interface EventDispatcherTarget {

  public String getName();

  public void dispatch(EventRule rule, Event event, String payload);

}
