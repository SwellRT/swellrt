package org.swellrt.server.box.events;

import com.google.inject.Inject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventDispatcherImpl implements EventDispatcher {

  private final EventQueue queue;

  private Map<EventRuleClass, Set<EventRule>> rules;

  private Map<EventRuleClass, Set<String>> rulesExpressionsPath;

  private Map<String, EventDispatcherTarget> targets;


  @Inject
  public EventDispatcherImpl(EventQueue queue) {
    this.queue = queue;
    this.targets = new HashMap<String, EventDispatcherTarget>();
    this.rules = new HashMap<EventRuleClass, Set<EventRule>>();
    this.rulesExpressionsPath = new HashMap<EventRuleClass, Set<String>>();
  }


  @Override
  public void initialize(Map<String, EventDispatcherTarget> targets, Collection<EventRule> rules) {

    this.targets = targets;

    for (EventRule r : rules) {

      // Group rules by rule class (= app, data type)

      EventRuleClass ruleClass = EventRuleClass.ofEventRule(r);

      if (!this.rules.containsKey(ruleClass)) {
        this.rules.put(ruleClass, new HashSet<EventRule>());
      }

      this.rules.get(ruleClass).add(r);


      // Get rules expressions and group by rule class

      if (!this.rulesExpressionsPath.containsKey(ruleClass)) {
        this.rulesExpressionsPath.put(ruleClass, new HashSet<String>());
      }

      this.rulesExpressionsPath.get(ruleClass).addAll(r.getExpressionsPaths());

    }

    queue.registerConfigurator(this);

    queue.registerListener(this);
  }


  @Override
  public void onEvent(Event event) {

    if (!rules.containsKey(EventRuleClass.ofEvent(event))) return;

    for (EventRule rule : rules.get(EventRuleClass.ofEvent(event))) {

      if (rule.match(event)) {
        for (String t : rule.getTargets()) {
          targets.get(t).dispatch(event, rule.getEventPayload(t, event));
        }
      }
    }
  }


  @Override
  public Set<EventRuleClass> getEventRuleClasses() {
    return rules.keySet();
  }


  @Override
  public Set<String> getExpressionPaths(EventRuleClass ruleClass) {
    return rulesExpressionsPath.get(ruleClass);
  }

  protected Map<String, EventDispatcherTarget> getTargets() {
    return targets;
  }

}
