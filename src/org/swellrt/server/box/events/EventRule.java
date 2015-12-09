package org.swellrt.server.box.events;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.swellrt.server.box.events.Event.Type;
import org.waveprotocol.wave.util.logging.Log;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A rule that match events.
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class EventRule {

  private static final Log LOG = Log.get(EventRule.class);


  public static Collection<EventRule> fromFile(String confFilePath) {


    FileReader fr;
    try {
      fr = new FileReader(confFilePath);
    } catch (FileNotFoundException e) {
      LOG.warning("Event rules definition's file not found: " + confFilePath);
      return Collections.<EventRule> emptyList();
    }
    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElement = jsonParser.parse(fr);

    if (!jsonElement.isJsonArray()) {
      LOG.warning("Event rules definition's file json array not found: " + confFilePath);
      return Collections.<EventRule> emptyList();
    }

    JsonArray eventArray = jsonElement.getAsJsonArray();

    List<EventRule> rules = new ArrayList<EventRule>();
    int s = 0;
    for (int i = 0; i < eventArray.size(); i++) {
      try {

        JsonObject eventJson = eventArray.get(i).getAsJsonObject();
        EventRule rule = fromJson(eventJson);
        if (rule != null) {
          s++;
          rules.add(rule);
        }

      } catch (RuntimeException e) {
        LOG.warning("Event rule #" + i + " parsing error" + confFilePath);
      }
    }

    LOG.info(s + " event rules loaded successfully");

    return rules;
  }

  public static EventRule fromJson(JsonObject jso) {

    EventRule rule = null;

    try {

      Map<String, String> conditionsMap = new HashMap<String, String>();


      JsonArray conditions = jso.getAsJsonArray("conditions");

      for (int i = 0; i < conditions.size(); i++) {
        Entry<String, JsonElement> element = conditions.get(i).getAsJsonObject().entrySet().iterator().next();
        conditionsMap.put(element.getKey(), element.getValue().getAsString());
      }

      rule =
            new EventRule(jso.get("id").getAsString(),
                jso.get("app").getAsString(),
                jso.get("dataType").getAsString(),
                Event.Type.valueOf(jso.get("type").getAsString()),
                jso.get("path").getAsString(),
                conditionsMap);



      Map<String, String> targetsMap = new HashMap<String, String>();
      JsonObject targets = jso.getAsJsonObject("targets");

      for(Entry<String, JsonElement> entry: targets.entrySet()) {
        targetsMap.put(entry.getKey(), entry.getValue().toString());
      }

      rule.setTargets(targetsMap);

    } catch (RuntimeException e) {

      LOG.warning("Error parsing event rule definition", e);
      return null;

    } catch (InvalidEventExpressionException e) {

      LOG.warning("Error parsing event rule definition", e);
      return null;
    }

    return rule;
  }


  private final String id;
  private final String app;
  private final String dataType;

  private Map<String, String> conditions;

  private final Event.Type type;
  private final String path;

  private Map<String, String> targets;

  /**
   * Rules can define path expresions to be evaluated against the data model
   * associated with an event.
   *
   * When a rule matches an event, EventRule can get the evaluation of these
   * expressions calling to
   * {@link EventRule#evaluateExpression(Event event, String expresion)}
   *
   * Expresions must be provided to the parent constructor in order to be
   * extracted when events are generated.
   *
   * Expresions are like this:
   *
   * ${root.key.0.value} ${root.key.?.value} $hash{root.key.0.value}
   *
   * the simple version '${path}', means 'the value on this path'. other
   * functions can be used with paths, like '$hash{}'
   *
   * ? is a wildcard for list indexes, allowing to extract values for the list
   * item matched by the event.
   *
   */
  private final Set<String> expressions = new HashSet<String>();
  private final Set<String> expressionsPaths = new HashSet<String>();



  public EventRule(String id, String app, String dataType, Type type, String path,
      Map<String, String> conditions)
      throws InvalidEventExpressionException {
    super();
    this.id = id;
    this.app = app;
    this.dataType = dataType;
    this.conditions = conditions;
    this.type = type;
    this.path = path;
  }

  public EventRule(String id, String app, String dataType, Type type, String path)
      throws InvalidEventExpressionException {
    super();
    this.id = id;
    this.app = app;
    this.dataType = dataType;
    this.type = type;
    this.path = path;
  }

  protected void setTargets(Map<String, String> targets) throws InvalidEventExpressionException {

    this.targets = targets;

    // Extract expressions from the JSON payload to provide them to the event
    // system in advance


    for (String payloadTemplate: targets.values()) {

      ExpressionParser parser = new ExpressionParser(payloadTemplate, new ExpressionParser.Operation() {

        @Override
            public String onExpression(String expression) {

          try {

                if (ExpressionParser.isPathExpresion(expression)) {

                  String expressionPath = ExpressionParser.extractExpressionPath(expression);
                  expressions.add(expression);
                  expressionsPaths.add(expressionPath);

                }

          } catch (InvalidEventExpressionException e) {
            LOG.warning("Error parsing JSON payload", e);
          }
              return null;
        }
      });

      parser.parse();

    }


  }



  protected boolean matchPath(Event event) {
    return ExpressionParser.comparePaths(path, event.getPath());
  }

  /**
   * Precondition paths don't admit wildcards. Hence they can't trasverse lists.
   *
   * @param event
   * @return
   */
  protected boolean matchPreconditions(Event event) {

    boolean doesItMatch = true;

    for (Entry<String, String> c : conditions.entrySet()) {
      doesItMatch =
          event.getContextData().containsKey(c.getKey())
              && event.getContextData().get(c.getKey()).equals(c.getValue());
      if (!doesItMatch) break;
    }

    return doesItMatch;
  }


  public boolean match(Event event) {

    Preconditions.checkNotNull(event, "Event can't be null");

    boolean doesItMatch = true;

    doesItMatch = (event.getApp().equals(app)) && (event.getDataType().equals(dataType));

    doesItMatch = doesItMatch && event.getType().equals(type);

    if (!doesItMatch) return false; // Avoid unnecessary further logic

    doesItMatch = doesItMatch && matchPath(event);

    if (!doesItMatch) return false; // Avoid unnecessary further logic

    // Match conditions. This should be avoided.
    // Use app and dataType as preconditions instead.

    doesItMatch = doesItMatch && matchPreconditions(event);

    return doesItMatch;
  }

  public Set<String> getExpressionsPaths() {
    // TODO include all expressions
    return conditions.keySet();
  }



  @Override
  public boolean equals(Object obj) {
    if (obj != null && obj instanceof EventRule) {
      EventRule er = (EventRule) obj;
      return this.id.equals(er.id) && this.app.equals(er.app) && this.dataType.equals(er.dataType);
    }
    return false;
  }

  public String getId() {
    return id;
  }

  public String getApp() {
    return app;
  }

  public String getDataType() {
    return dataType;
  }

  public Set<String> getTargets() {
    return targets.keySet();
  }

  public Type getType() {
    return type;
  }

  public String getPath() {
    return path;
  }


  public String getEventPayload(String target, final Event event) {

    ExpressionParser ep = new ExpressionParser(targets.get(target), new ExpressionParser.Operation() {

          @Override
          public String onExpression(String expression) {
            try {
              return ExpressionParser.evaluateExpression(event, expression);
            } catch (InvalidEventExpressionException e) {
              LOG.warning("Error evaluating expression for event", e);
            }
            return "";
          }

    });

    String payload = ep.replaceParse();

    // TODO We could parse as Json the calcultared payload to verify correctness

    return payload;

  }

}
