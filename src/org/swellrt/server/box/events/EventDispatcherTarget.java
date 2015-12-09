package org.swellrt.server.box.events;


public interface EventDispatcherTarget {

  public String getName();

  public void dispatch(Event event, String payload);

}
