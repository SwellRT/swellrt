package org.swellrt.model.adt;

import junit.framework.TestCase;

import org.swellrt.model.adt.UnmutableElementList.ElementAdapter;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.testing.BasicFactories;

import java.util.Iterator;


/**
 * 
 * @author pablojan@gmail.com
 * 
 */
public class UnmutableElementListTest extends TestCase {


  protected UnmutableElementList<String, Void> list;

  protected void setUp() throws Exception {
    super.setUp();
    buildListHelper();
  }


  private String[] expectedValues = {"Value X", "Value Y", "Value Z"};

  @SuppressWarnings("unchecked")
  protected void buildListHelper() {

    final Document document =
        BasicFactories.documentProvider().parse(
            "<strings><s v='Value X'/><s v='Value Y'/><s v='Value Z'/></strings>");

    Doc.E parent = document.asElement(document.getFirstChild(document.getDocumentElement()));

    list =
        (UnmutableElementList<String, Void>) UnmutableElementList.create(
            new ElementAdapter<String>() {

      @Override
      public String fromElement(E element) {
          return document.getAttribute(element, "v");
        }
    },
    parent,
    document);


  }

  protected int indexOfExpectedValue(String value) {
    int i = 0;
    while (i < expectedValues.length) {
      if (expectedValues[i].equals(value)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  public void testGetValues() {
    Iterator<String> it = list.getValues().iterator();
    int sumOfExpectedIndex = 0;

    while (it.hasNext()) {
      String value = it.next();
      int expectedValueIndex = indexOfExpectedValue(value);
      sumOfExpectedIndex += expectedValueIndex;
      assertTrue(expectedValueIndex != -1);
    }
    assertEquals(3, sumOfExpectedIndex); // Test if we've got all values
  }

  public void testIndexOf() {
    assertEquals(0, list.indexOf(list.get(0)));
    assertEquals(1, list.indexOf(list.get(1)));
    assertEquals(2, list.indexOf(list.get(2)));
  }

  public void testGet() {
    assertEquals(expectedValues[0], list.get(0));
    assertEquals(expectedValues[1], list.get(1));
    assertEquals(expectedValues[2], list.get(2));
  }

  public void testSize() {
    assertEquals(3, list.size());
  }

}
