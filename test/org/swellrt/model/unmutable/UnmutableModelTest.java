package org.swellrt.model.unmutable;



import org.swellrt.model.ReadableList;
import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableString;
import org.swellrt.model.ReadableType;
import org.swellrt.model.WaveletBasedTestBase;
import org.swellrt.model.adt.UnmutableElementList;

import java.util.Iterator;

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


  public void testTypeFactory() {

    UnmutableModel umodel = UnmutableModel.create(getWaveletData());

    // Testing Model

    // Testing string list
    UnmutableElementList<String, Void> strings = umodel.strings();
    assertEquals(6, strings.size());

    for (int i = 0; i < strings.size(); i++) {
      assertEquals("This is the string " + i, strings.get(i));
    }

    // Testing Map

    ReadableMap rootMap = umodel.getRoot();
    assertTrue(rootMap.keySet().contains("keymap"));
    assertTrue(rootMap.keySet().contains("keylist"));
    assertTrue(rootMap.keySet().contains("keystring"));

    // Testing List

    ReadableList list = (ReadableList) rootMap.get("keylist");
    assertEquals(3, list.size());

    Iterator<ReadableType> it = list.getValues().iterator();
    assertTrue(it.hasNext());
    assertTrue(it.next() instanceof ReadableMap);

    assertTrue(it.hasNext());
    assertTrue(it.next() instanceof ReadableList);

    assertTrue(it.hasNext());
    assertTrue(it.next() instanceof ReadableString);

    assertFalse(it.hasNext());

    // Test String

    ReadableString str = (ReadableString) list.get(2);
    assertEquals("This is the string 3", str.getValue());

  }
}
