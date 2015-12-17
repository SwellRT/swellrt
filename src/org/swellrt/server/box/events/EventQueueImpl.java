package org.swellrt.server.box.events;

import org.waveprotocol.wave.util.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventQueueImpl implements EventQueue {

  private static final Log LOG = Log.get(EventQueueImpl.class);

  private List<EventQueueListener> listeners = new ArrayList<EventQueueListener>();

  private Map<EventRuleClass, Set<String>> expressions =
 new HashMap<EventRuleClass, Set<String>>();

  @Override
  public void add(Event event) {

    LOG.info("Added new event to queue: " + event);

    for (EventQueueListener l : listeners)
      l.onEvent(event);
  }

  @Override
  public void registerListener(EventQueueListener listener) {
    listeners.add(listener);
  }

  @Override
  public boolean hasEvents(String app, String dataType) {
    return expressions.containsKey(new EventRuleClass(app, dataType));
  }

  @Override
  public Set<String> getExpressionPaths(String app, String dataType) {
    return expressions.get(new EventRuleClass(app, dataType));
  }

  @Override
  public void registerConfigurator(EventQueueConfigurator configurator) {

    for (EventRuleClass appAndType : configurator.getEventRuleClasses()) {

      Set<String> paths = expressions.get(appAndType);
      if (paths == null) {
        paths = new HashSet<String>();
        expressions.put(appAndType, paths);
      }
      paths.addAll(configurator.getExpressionPaths(appAndType));

    }

  }

}
