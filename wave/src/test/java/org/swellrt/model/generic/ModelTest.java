package org.swellrt.model.generic;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class ModelTest extends TestCase {


  private FakeWaveView wave;

  public void setUp() throws Exception {
    wave = BasicFactories.fakeWaveViewBuilder().build();
  }

  /**
   * Test public methods of the Model and Types classes
   */
  public void testModel() {

    Model model =
        Model.create(wave, "local.net", ParticipantId.ofUnsafe("fake@local.net"), true,
        new IdGeneratorImpl(
        "local.net", new Seed() {
          @Override
          public String get() {
            return "";
          }
        }));

    MapType root = model.getRoot();
    assertNotNull(root);

    // Root Map

    root.put("r0", model.createString("Hello World One"));
    root.put("r1", model.createList());
    root.put("r2", model.createMap());
    root.put("r3", model.createString("Hello World Two"));
    root.put("r4", "Hello World Three");
    root.put("r5", model.createText("foo"));

    assertTrue(root.get("r0") instanceof StringType);
    assertEquals("Hello World One", ((StringType) root.get("r0")).getValue());

    assertTrue(root.get("r1") instanceof ListType);

    assertTrue(root.get("r2") instanceof MapType);

    // Test recursive access to an object
    MapType map = (MapType) root.get("r2");
    map.put("m0", "La La Lo");
    assertEquals("La La Lo", ((StringType) ((MapType) root.get("r2")).get("m0")).getValue());

    assertEquals("root.r2", map.getPath());

    assertTrue(root.get("r3") instanceof StringType);
    assertEquals("Hello World Two", ((StringType) root.get("r3")).getValue());

    assertTrue(root.get("r4") instanceof StringType);
    assertEquals("Hello World Three", ((StringType) root.get("r4")).getValue());

    assertTrue(root.get("r5") instanceof TextType);
    assertEquals("<body path=\"root.r5\"><line/>foo</body>", ((TextType) root.get("r5")).getXml());

    // List

    ListType list = (ListType) root.get("r1");

    assertEquals("root.r1", list.getPath());

    list.add(model.createString("String 0"));
    list.add(model.createString("String 1"));
    list.add(model.createList());
    list.add(model.createMap());
    list.add(model.createText("bar"));
    list.add(model.createString("String 2"));
    list.add(model.createString("String 3"));

    assertEquals(7, list.size());
    assertEquals("String 0", ((StringType) list.get(0)).getValue());
    assertEquals("String 1", ((StringType) list.get(1)).getValue());
    assertTrue(list.get(2) instanceof ListType);
    assertTrue(list.get(3) instanceof MapType);
    assertEquals("<body path=\"root.r1.4\"><line/>bar</body>", ((TextType) list.get(4)).getXml());
    assertEquals("String 2", ((StringType) list.get(5)).getValue());
    assertEquals("String 3", ((StringType) list.get(6)).getValue());

    // Test recursive access to an object
    assertEquals("String 3", ((StringType) ((ListType) root.get("r1")).get(6)).getValue());

    //
    assertNotNull(((ListType) root.get("r1")).getValues());
  }


}
