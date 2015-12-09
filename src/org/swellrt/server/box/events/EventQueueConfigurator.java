package org.swellrt.server.box.events;

import java.util.Set;

public interface EventQueueConfigurator {

  public Set<EventRuleClass> getEventRuleClasses();

  public Set<String> getExpressionPaths(EventRuleClass ruleClass);

}
