package org.swellrt.server.box.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class EventRuleTest extends TestCase {

  protected void setUp() throws Exception {
    super.setUp();

  }

  protected EventRule createEventRule() throws UnsupportedEncodingException {


      InputStreamReader isr = new InputStreamReader(this.getClass().getResourceAsStream("EventRuleTest_Rule_1.json"),
            "UTF-8");


      JsonParser jsonParser = new JsonParser();
      JsonElement jsonElement = jsonParser.parse(isr);

      return EventRule.fromJson(jsonElement.getAsJsonObject());

  }

  protected String getExpectedGcmPayload() throws IOException {
    InputStreamReader isr =
        new InputStreamReader(this.getClass().getClassLoader()
            .getResourceAsStream(
                "org/swellrt/server/box/events/EventRuleTest_Rule_1_payload_gcm.json"),
            "UTF-8");

    return IOUtils.toString(isr);
  }

  public void testComparePaths() {
    assertTrue(ExpressionParser.comparePaths("root.field.subfield", "root.field.subfield"));
    assertTrue(ExpressionParser.comparePaths("root", "root"));
    assertFalse(ExpressionParser.comparePaths("root", "root.field"));
    assertTrue(ExpressionParser.comparePaths("root.list.?.field", "root.list.5.field"));
    assertTrue(ExpressionParser.comparePaths("root.list.?.list.?", "root.list.2.list.?"));
  }

  public void testExtractExpressionPath() throws InvalidEventExpressionException {

    String exp1 = "${root.map.field}";
    assertEquals("root.map.field", ExpressionParser.extractExpressionPath(exp1));

    String exp2 = "${root.map.?.field}";
    assertEquals("root.map.?.field", ExpressionParser.extractExpressionPath(exp2));

    String exp4 = "${root.field1}";
    assertEquals("root.field1", ExpressionParser.extractExpressionPath(exp4));

    String exp5 = "${root.field_slash}";
    assertEquals("root.field_slash", ExpressionParser.extractExpressionPath(exp5));

  }


  public void testFromJson() throws UnsupportedEncodingException {

    EventRule rule = createEventRule();

    assertEquals("event_rule_01", rule.getId());
    assertEquals("default", rule.getDataType());
    assertEquals("default", rule.getApp());
    assertEquals(Event.Type.MAP_ENTRY_UPDATED, rule.getType());
    assertEquals("root.map.flag", rule.getPath());

    assertEquals(2, rule.getExpressionsPaths().size());
    assertTrue(rule.getExpressionsPaths().contains("root.field1"));
    assertTrue(rule.getExpressionsPaths().contains("root.field2"));

    assertTrue(rule.getTargets().contains("gcm"));
    assertTrue(rule.getTargets().contains("email"));


  }


  public void testEvaluateExpression() throws InvalidEventExpressionException,
      InvalidParticipantAddress {

    Map<String, String> contextData = new HashMap<String, String>();
    contextData.put("root.field1", "value1");
    contextData.put("root.list.?.field2", "value2");

    Event.Builder b = new Event.Builder();
    b.app("default").author("dummy@example.com").blipId("").dataType("default")
        .timestamp(9999L)
        .contextData(contextData);

    Event e = b.build(Event.Type.MAP_ENTRY_UPDATED, "root.list.?.field2");

    assertEquals("value1", ExpressionParser.evaluateExpression(e, "${root.field1}"));
    assertEquals("value2", ExpressionParser.evaluateExpression(e, "${root.list.?.field2}"));
    assertEquals("dummy@example.com", ExpressionParser.evaluateExpression(e, "$author_id"));
    assertEquals("dummy", ExpressionParser.evaluateExpression(e, "$author"));
    assertEquals("9999", ExpressionParser.evaluateExpression(e, "$timestamp"));

    e = b.buildAddParticipant(ParticipantId.of("joe@example.com"));
    assertEquals("joe", ExpressionParser.evaluateExpression(e, "$participant"));
    assertEquals("joe@example.com", ExpressionParser.evaluateExpression(e, "$participant_id"));

    e = b.buildRemoveParticipant(ParticipantId.of("joe@example.com"));
    assertEquals("joe", ExpressionParser.evaluateExpression(e, "$participant"));
    assertEquals("joe@example.com", ExpressionParser.evaluateExpression(e, "$participant_id"));

  }


  public void testGetEventPayload() throws IOException {

    EventRule rule = createEventRule();
    String expectedPayload = getExpectedGcmPayload();

    Map<String, String> contextData = new HashMap<String, String>();
    contextData.put("root.field1", "value");
    contextData.put("root.field2", "value of the second field");

    Event.Builder b = new Event.Builder();
    b.app("default").author("dummy@example.com").blipId("").dataType("default")
        .timestamp(9999L).contextData(contextData);

    Event event = b.build(Event.Type.MAP_ENTRY_UPDATED, "root.field2");

    String payload = rule.getEventPayload("gcm", event);

    assertEquals(expectedPayload, payload);

  }

}
