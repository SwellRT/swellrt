package org.swellrt.model.generic;

import junit.framework.TestCase;

import org.swellrt.model.generic.ModelMigrator.VersionNumber;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;


public class ModelMigratorTest extends TestCase {

  private FakeWaveView view;

  public ModelMigratorTest(String name) {
    super(name);
  }


  /**
   * Utility to set xml of a blip
   * 
   * @param blip
   * @param xml
   */
  protected void setBlipXmlContent(Blip blip, String xml) {
    blip.getContent().appendXml(XmlStringBuilder.createFromXmlString(xml));
  }

  /**
   * Inform a Wave with dummy data for the v0.2 swellrt data model
   *
   * @param wave
   */
  protected void createWaveletDataModel_v_0_2(String domain, ObservableWaveView wave) {

    ObservableWavelet wavelet = wave.createWavelet(WaveletId.of(domain, "swl+root"));

    // Data Model Sample XML
    // Recursiveness cases: map->list->map, map->map->list, map->list->list

    String xmlModelStart = "<model v='0.2'>";
    String xmlModelEnd = "</model>";
    String xmlStrings =
        "<strings>" + "<s v='This is string 0' />" + "<s v='This is string 1' />"
            + "<s v='This is string 2' />" + "<s v='This is string 3' />"
            + "<s v='This is string 4' />" + "<s v='This is string 5' />" + "</strings>";

    String xmlRoot =
        "<map>" + "<entry k='key0' v='str+0' />" + "<entry k='key1' v='map+0001' />"
            + "<entry k='key2' v='list+0001' />" + "<entry k='key3' v='str+1' />"
            + "<entry k='key4' v='map+0002' />"
            + "</map>";

    String xmlMap1 =
        "<map>" + "<entry k='key10' v='str+2' />" + "<entry k='key11' v='list+0002' />" + "</map>";

    String xmlMap2 =
        "<map>" + "<entry k='key20' v='str+3' />" + "<entry k='key21' v='map+0003' />" + "</map>";

    String xmlMap3 = "<map></map>";


    String xmlList1 =
        "<list>" + "<item t='str' r='str+4' />" + "<item t='str' r='str+5' />"
            + "<item t='map' r='map+0004'/>" + "<item t='list' r='list+0003'/>"
            + "</list>";

    String xmlList2 = "<list></list>";

    String xmlList3 = "<list></list>";

    String xmlMap4 = "<map></map>";


    setBlipXmlContent(wavelet.createBlip("model+root"), xmlModelStart + xmlStrings + xmlRoot
        + xmlModelEnd);

    setBlipXmlContent(wavelet.createBlip("map+0001"), xmlMap1);

    setBlipXmlContent(wavelet.createBlip("list+0001"), xmlList1);

    setBlipXmlContent(wavelet.createBlip("map+0002"), xmlMap2);

    setBlipXmlContent(wavelet.createBlip("list+0002"), xmlList2);

    setBlipXmlContent(wavelet.createBlip("map+0003"), xmlMap3);

    setBlipXmlContent(wavelet.createBlip("list+0003"), xmlList3);

    setBlipXmlContent(wavelet.createBlip("map+0004"), xmlMap4);


  }


  /**
   * Perform pre-test initialization
   *
   * @throws Exception
   *
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();

    view = BasicFactories.fakeWaveViewBuilder().build();

  }

  protected void subTestMetadata(Blip blip, String expectedPath) {

    Document doc = blip.getContent();

    Doc.E elementMeta = DocHelper.getElementWithTagName(doc, "metadata");
    assertNotNull(elementMeta);
    assertNotNull(doc.getAttribute(elementMeta, "pc"));
    assertNotNull(doc.getAttribute(elementMeta, "tc"));
    assertNotNull(doc.getAttribute(elementMeta, "pm"));
    assertNotNull(doc.getAttribute(elementMeta, "tm"));
    assertNotNull(doc.getAttribute(elementMeta, "ap"));
    assertNotNull(doc.getAttribute(elementMeta, "acl"));
    assertEquals(expectedPath, doc.getAttribute(elementMeta, "p"));

  }

  /**
   * Test migration process from v0.2 to v1.0
   */
  public void testMigrate_0_2_to_1_0() {

    String DOMAIN = "local.net";

    // Create dummy data following v0.2
    createWaveletDataModel_v_0_2(DOMAIN, view);


    // Check version
    try {

      VersionNumber currentVersion;
      currentVersion = ModelMigrator.getVersionNumber(DOMAIN, view);
      // assertEquals(new VersionNumber(0, 2), currentVersion);
      assertTrue(currentVersion.equals(new VersionNumber(0, 2)));
      assertTrue((new VersionNumber(0, 2)).equals(currentVersion));

    } catch (NotModelWaveException e) {
      assertTrue("Exception getting version", false);
    }


    // Run migration to v1.0
    ModelMigrator.migrate_0_2_to_1_0(DOMAIN, view);


    // Wavelet
    ObservableWavelet w = view.getWavelet(WaveletId.of(DOMAIN, "swl+root"));

    // model+root
    Document docModelRoot = w.getBlip("model+root").getContent();

    // New model attributes
    Doc.E elementModel =
        DocHelper.getFirstChildElement(docModelRoot, docModelRoot.getDocumentElement());

    assertEquals("1.0", docModelRoot.getAttribute(elementModel, "v"));
    assertEquals("default", docModelRoot.getAttribute(elementModel, "t"));
    assertEquals("default", docModelRoot.getAttribute(elementModel, "a"));

    // Check removal of string index
    assertNull(DocHelper.getElementWithTagName(docModelRoot, "strings"));

    // Check for no root map
    assertNull(DocHelper.getElementWithTagName(docModelRoot, "map"));

    // Check for the new map+root blip
    Blip blipMapRoot = w.getBlip("map+root");
    assertNotNull(blipMapRoot);

    // Check content of map+root is equivalent to original map on model+root
    Document docMapRoot = blipMapRoot.getContent();

    // Map tag exists / it checks also string values migration from index
    assertNotNull(DocHelper.getElementWithTagName(docMapRoot, "map"));

    Doc.E elementMap =
        DocHelper.getFirstChildElement(docMapRoot,
            DocHelper.getElementWithTagName(docMapRoot, "map"));

    Doc.E elementValues =
        DocHelper.getFirstChildElement(docMapRoot,
            DocHelper.getElementWithTagName(docMapRoot, "values"));

    assertEquals("key0", docMapRoot.getAttribute(elementMap, "k"));
    assertEquals("str+0", docMapRoot.getAttribute(elementMap, "v"));
    assertEquals("This is string 0", docMapRoot.getAttribute(elementValues, "v"));


    elementMap = DocHelper.getNextSiblingElement(docMapRoot, elementMap);
    assertEquals("key1", docMapRoot.getAttribute(elementMap, "k"));
    assertEquals("map+0001", docMapRoot.getAttribute(elementMap, "v"));

    elementMap = DocHelper.getNextSiblingElement(docMapRoot, elementMap);
    assertEquals("key2", docMapRoot.getAttribute(elementMap, "k"));
    assertEquals("list+0001", docMapRoot.getAttribute(elementMap, "v"));

    elementMap = DocHelper.getNextSiblingElement(docMapRoot, elementMap);
    assertEquals("key3", docMapRoot.getAttribute(elementMap, "k"));
    assertEquals("str+1", docMapRoot.getAttribute(elementMap, "v"));

    elementValues = DocHelper.getNextSiblingElement(docMapRoot, elementValues);
    assertEquals("This is string 1", docMapRoot.getAttribute(elementValues, "v"));

    elementMap = DocHelper.getNextSiblingElement(docMapRoot, elementMap);
    assertEquals("key4", docMapRoot.getAttribute(elementMap, "k"));
    assertEquals("map+0002", docMapRoot.getAttribute(elementMap, "v"));


    // Check migration of values in a list
    Blip blipList1 = w.getBlip("list+0001");
    Document docList1 = blipList1.getContent();

    Doc.E elementList =
        DocHelper.getFirstChildElement(docList1, DocHelper.getElementWithTagName(docList1, "list"));

    elementValues =
        DocHelper.getFirstChildElement(docList1,
            DocHelper.getElementWithTagName(docList1, "values"));

    assertEquals("str", docList1.getAttribute(elementList, "t"));
    assertEquals("str+0", docList1.getAttribute(elementList, "r"));
    assertEquals("This is string 4", docList1.getAttribute(elementValues, "v"));

    elementList = DocHelper.getNextSiblingElement(docList1, elementList);
    assertEquals("str", docList1.getAttribute(elementList, "t"));
    assertEquals("str+1", docList1.getAttribute(elementList, "r"));

    elementValues = DocHelper.getNextSiblingElement(docList1, elementValues);
    assertEquals("This is string 5", docList1.getAttribute(elementValues, "v"));

    elementList = DocHelper.getNextSiblingElement(docList1, elementList);
    assertEquals("map", docList1.getAttribute(elementList, "t"));
    assertEquals("map+0004", docList1.getAttribute(elementList, "r"));

    elementList = DocHelper.getNextSiblingElement(docList1, elementList);
    assertEquals("list", docList1.getAttribute(elementList, "t"));
    assertEquals("list+0003", docList1.getAttribute(elementList, "r"));

    // Check new metadata section in maps
    subTestMetadata(blipMapRoot, "root");

    // Check new metadata, path
    subTestMetadata(w.getBlip("map+0001"), "root.key1");

    // Check new metadata, path
    subTestMetadata(w.getBlip("list+0002"), "root.key1.key11");

    // Check new metadata, path
    subTestMetadata(w.getBlip("map+0004"), "root.key2.2");


  }
}
