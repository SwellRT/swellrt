package org.swellrt.server.box.events.dummy;

import org.swellrt.server.box.events.Event;
import org.swellrt.server.box.events.EventDispatcher;
import org.swellrt.server.box.events.EventDispatcherTarget;
import org.swellrt.server.box.events.EventRule;
import org.waveprotocol.wave.util.logging.Log;

import com.google.inject.Inject;

/**
 * A dummy dispatcher target for debugging. It logs event info.
 * 
 * @author pablojan@gmail.com
 * 
 */
public class DummyDispatcher implements EventDispatcherTarget {

  private static final Log LOG = Log.get(DummyDispatcher.class);

  public static final String NAME = "dummy";

  @Inject 
  public DummyDispatcher() {
  }
  
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void dispatch(EventRule rule, Event event, String payload) {

    LOG.info("Event dispatched to DUMMY: " + rule + " on " + event + " with payload " + payload);

  }

}
