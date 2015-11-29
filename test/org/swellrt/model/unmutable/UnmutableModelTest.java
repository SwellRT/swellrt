package org.swellrt.model.unmutable;



import org.swellrt.model.ReadableList;
import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableString;
import org.swellrt.model.ReadableText;
import org.swellrt.model.ReadableType;
import org.swellrt.model.WaveletBasedTestBase;

/**
 * Test UnmutableModel hierarchy (org.swellrt.model.adt.unmutable.*)
 *
 * @author pablojan
 *
 */
public class UnmutableModelTest extends WaveletBasedTestBase {




  protected void setUp() throws Exception {
    super.setUp();
  }


  @SuppressWarnings("rawtypes")
  public void testTypeFactory() {

    UnmutableModel umodel = UnmutableModel.create(getWaveletData());

    // ROOT

    ReadableMap root = umodel.getRoot();
    assertTrue(root.get("key0") instanceof ReadableString);
    assertTrue(root.get("key1") instanceof ReadableMap);
    assertTrue(root.get("key2") instanceof ReadableList);
    assertTrue(root.get("key3") instanceof ReadableString);
    assertTrue(root.get("key4") instanceof ReadableMap);
    assertTrue(root.get("key5") instanceof ReadableText);

    assertEquals("This is string 0", ((ReadableString) root.get("key0")).getValue());
    assertEquals("This is string 1", ((ReadableString) root.get("key3")).getValue());

    // LEVEL 1

    ReadableMap map1 = (ReadableMap) root.get("key1");
    assertEquals("This is string 2", ((ReadableString) map1.get("key10")).getValue());
    assertTrue(map1.get("key11") instanceof ReadableList);
    assertEquals(0, ((ReadableList) map1.get("key11")).size());

    @SuppressWarnings("unchecked")
    ReadableList<ReadableType> list1 = (ReadableList<ReadableType>) root.get("key2");
    assertEquals("This is string 4", ((ReadableString) list1.get(0)).getValue());
    assertEquals("This is string 5", ((ReadableString) list1.get(1)).getValue());
    assertEquals(0, ((ReadableMap) list1.get(2)).keySet().size());
    assertEquals(0, ((ReadableList) list1.get(3)).size());

    ReadableMap map2 = (ReadableMap) root.get("key4");

    ReadableText text1 = (ReadableText) root.get("key5");
    assertEquals("<body><line/>foo</body>", text1.getXml());
  }
}
