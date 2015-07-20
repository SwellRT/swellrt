package org.swellrt.model.adt;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.testing.BasicFactories;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author pablojan@gmail.com
 * 
 */
public class UnmutableBasicMapTest extends TestCase {


  protected UnmutableBasicMap<String, String> map;

  protected void setUp() throws Exception {
    super.setUp();
    buildMapHelper();
  }


  @SuppressWarnings("unchecked")
  protected void buildMapHelper() {

    final Document document =
        BasicFactories
            .documentProvider()
            .parse(
                "<map><entry k='keyOne' v='Value X'/><entry k='keyTwo' v='Value Y'/><entry k='keyThree' v='Value Z'/></map>");

    Doc.E parent = document.asElement(document.getFirstChild(document.getDocumentElement()));

    map =
        (UnmutableBasicMap<String, String>) UnmutableBasicMap.create(new UnmutableBasicMap.ElementAdapter<String, String>() {

          @Override
          public Entry<String, String> fromElement(final E element) {

            return new Map.Entry<String, String>() {

              @Override
              public String getKey() {
                    return document.getAttribute(element, "k");
              }

              @Override
              public String getValue() {
                    return document.getAttribute(element, "v");
              }

              @Override
              public String setValue(String value) {
                return null;
              }

                };
          }
        }, parent, document);


  }


  public void testGet() {
    assertEquals("Value X", map.get("keyOne"));
    assertEquals("Value Y", map.get("keyTwo"));
    assertEquals("Value Z", map.get("keyThree"));
  }

  public void testKeySet() {
    Set<String> keySet = map.keySet();
    assertEquals(3, keySet.size());
    assertTrue(keySet.contains("keyOne"));
    assertTrue(keySet.contains("keyTwo"));
    assertTrue(keySet.contains("keyThree"));
  }

}
