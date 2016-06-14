package org.swellrt.model;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.Factory;
import org.waveprotocol.wave.model.testing.WaveletDataFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.Collections;

/**
 * Provides a Wavelet backing up a SwellRT data model
 *
 * @author pablojan
 *
 */
public abstract class WaveletBasedTestBase extends TestCase {

  /*
   * The Wavelet stores the following data model structure:
   *
   *  (model+root)
   *
   *  root (map+root)
   *   |
   *   |--- key0 (string) 'This is string 0'
   *   |
   *   |--- key1 (map+0001)
   *   |      |
   *   |      |--- key10 (string) 'This is string 2'
   *   |      |
   *   |      |--- key11 (list+0002)
   *   |
   *   |
   *   |--- key2 (list+0001)
   *   |      |
   *   |      |--- 0 (string) 'This is string 4'
   *   |      |
   *   |      |
   *   |      |--- 1 (string) 'This is string 5'
   *   |      |
   *   |      |
   *   |      |--- 2 (map+0004)
   *   |      |
   *   |      |
   *   |      |----3 (list+0003)
   *   |
   *   |--- key3 (string) 'This is string 1'
   *   |
   *   |
   *   |
   *   |--- key4 (map+0002)
   *   |      |
   *   |      |--- key20 (string) 'This is string 3'
   *   |      |
   *   |      |--- key21 (map+0003)
   *   |
   *   |--- key5 (b+0001) '<body><line/>foo</body>'
   *
   */

  static String MODEL = "<model a='default' t='default' v='1.0'/>";

  // ROOT

  static String MAP_ROOT =
      "<metadata acl='' ap='default' p='root' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448723749565'/>"
          + "<map>"
          + "<entry k='key0' v='str+0'/>"
          + "<entry k='key1' v='map+0001'/>"
          + "<entry k='key2' v='list+0001'/>"
          + "<entry k='key3' v='str+1'/>"
          + "<entry k='key4' v='map+0002'/>"
          + "<entry k='key5' v='b+0001'/>"
          + "</map>"
          + "<values>"
          + "<i v='This is string 0'/>"
          + "<i v='This is string 1'/>" + "</values>";

  // LEVEL 1

  static String MAP_0001
 =
      "<metadata acl='' ap='default' p='root.key1' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448723749575'/>"
          + "<map>"
          + "<entry k='key10' v='str+0'/>"
          + "<entry k='key11' v='list+0002'/>"
          + "</map>" + "<values>" + "<i v='This is string 2'/>" + "</values>";


  static String LIST_0001 =
      "<metadata acl='' ap='default' p='root.key2' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448724445151'/>"
          + "<list>"
          + "<item r='str+0' t='str'/>"
          + "<item r='str+1' t='str'/>"
          + "<item r='map+0004' t='map'/>"
          + "<item r='list+0003' t='list'/>"
          + "</list>"
          + "<values>" + "<i v='This is string 4'/>" + "<i v='This is string 5'/>" + "</values>";

  static String MAP_0002 =
      "<metadata acl='' ap='default' p='root.key4' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448723749627'/>"
          + "<map>"
          + "<entry k='key20' v='str+0'/>"
          + "<entry k='key21' v='map+0003'/>"
          + "</map>"
          + "<values>" + "<i v='This is string 3'/>" + "</values>";

  static String B_0001 = "<body><line/>foo</body>";

  // LEVEL 2

  static String LIST_0002 =
      "<metadata acl='' ap='default' p='root.key1.key11' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448723749586'/>"
          + "<list/><values/>";

  static String MAP_0004 =
      "<metadata acl='' ap='default' p='root.key2.2' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448723749608'/>"
          + "<map/><values/>";

  static String LIST_0003 =
      "<metadata acl='' ap='default' p='root.key2.3' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448723749617'/>"
          + "<list/><values/>";

  static String MAP_0003 =
      "<metadata acl='' ap='default' p='root.key4.key21' pc='fake@example.com' pm='_system_@local.net' tc='0' tm='1448723749638'/>"
          + "<map/><values/>";


  // LEVEL 3


  private final Factory<WaveletDataImpl> factory =
      WaveletDataFactory.of(BasicFactories.waveletDataImplFactory());

  private WaveletData waveletData;


  protected void addDocumentFromXml(WaveletData wavelet, ParticipantId creator, String docId,
      String xml) {

    DocInitialization content = BasicFactories.documentProvider().parse(xml).toInitialization();
    waveletData.createDocument(docId, creator, Collections.singleton(creator), content,
        System.currentTimeMillis(), 0);

  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    waveletData = factory.create();
    waveletData.addParticipant(ParticipantId.of("tom@example.com"));
    waveletData.addParticipant(ParticipantId.of("tim@example.com"));

    ParticipantId creator = ParticipantId.of("fake@example.com");

    addDocumentFromXml(waveletData, creator, "model+root", MODEL);
    addDocumentFromXml(waveletData, creator, "map+root", MAP_ROOT);

    addDocumentFromXml(waveletData, creator, "map+0001", MAP_0001);
    addDocumentFromXml(waveletData, creator, "list+0001", LIST_0001);
    addDocumentFromXml(waveletData, creator, "map+0002", MAP_0002);
    addDocumentFromXml(waveletData, creator, "b+0001", B_0001);

    addDocumentFromXml(waveletData, creator, "list+0002", LIST_0002);
    addDocumentFromXml(waveletData, creator, "map+0004", MAP_0004);
    addDocumentFromXml(waveletData, creator, "list+0003", LIST_0003);
    addDocumentFromXml(waveletData, creator, "map+0003", MAP_0003);

  }

  protected WaveletData getWaveletData() {
    return waveletData;
  }


}
