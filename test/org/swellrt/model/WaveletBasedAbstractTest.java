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
public abstract class WaveletBasedAbstractTest extends TestCase {

  /*
   * The Wavelet stores the following data model structure:
   *
   *  root (map)
   *   |
   *   |--- keymap (map)
   *   |      |
   *   |      |--- keyone (string)
   *   |      |
   *   |      |--- keytwo (string)
   *   |
   *   |
   *   |--- keylist (list)
   *   |      |
   *   |      |--- 0 (map)
   *   |      |    |
   *   |      |    --- keyone (string)
   *   |      |
   *   |      |--- 1 (list)
   *   |      |    |
   *   |      |    --- 0 (string)
   *   |      |
   *   |      ---- 2 (string)
   *   |
   *   ---- keystring (string)
   *
   */

  static String MODEL_ROOT_DOC_CONTENT
 = "<model v='0.2'>" +
      "<strings>"+
          "<s v='This is the string 0'/>"+
          "<s v='This is the string 1'/>"+
          "<s v='This is the string 2'/>"+
          "<s v='This is the string 3'/>"+
          "<s v='This is the string 4'/>"+
          "<s v='This is the string 5'/>"+
      "</strings>"+
      "<map>"+
          "<entry k='keymap' v='map+MAP0001'/>"+
          "<entry k='keylist' v='list+LIST0001' />"+
          "<entry k='keystring' v='str+0' />"+
      "</map>"+
    "</model>";


  static String MAP_01_DOC_CONTENT
    = "<map>"+
        "<entry k='keyone' v='str+1' />"+
        "<entry k='keytwo' v='str+2' />"+
    "</map>";

  static String LIST_01_DOC_CONTENT = "<list>"+
        "<item r='map+MAP0002' t='map'/>"+
        "<item r='list+LIST0002' t='list'/>"+
        "<item r='str+3' t='str'/>"+
    "</list>";

  static String MAP_02_DOC_CONTENT = "<map><entry k='keyone' v='str+4' /></map>";

  static String LIST_02_DOC_CONTENT = "<list><item r='str+5' t='str'/></list>";

  private final Factory<WaveletDataImpl> factory =
      WaveletDataFactory.of(BasicFactories.waveletDataImplFactory());

  private WaveletData waveletData;

  protected void setUp() throws Exception {
    super.setUp();

    waveletData = factory.create();
    waveletData.addParticipant(ParticipantId.of("tom@example.com"));
    waveletData.addParticipant(ParticipantId.of("tim@example.com"));

    ParticipantId creator = ParticipantId.of("creator@example.com");
    DocInitialization content = BasicFactories.documentProvider().parse(MODEL_ROOT_DOC_CONTENT).toInitialization();

    waveletData.createDocument("model+root", creator, Collections.singleton(creator), content,
        System.currentTimeMillis(), 0);


    content = BasicFactories.documentProvider().parse(MAP_01_DOC_CONTENT).toInitialization();
    waveletData.createDocument("map+MAP0001", creator, Collections.singleton(creator), content,
        System.currentTimeMillis(), 0);

    content = BasicFactories.documentProvider().parse(LIST_01_DOC_CONTENT).toInitialization();
    waveletData.createDocument("list+LIST0001", creator, Collections.singleton(creator), content,
        System.currentTimeMillis(), 0);

    content = BasicFactories.documentProvider().parse(MAP_02_DOC_CONTENT).toInitialization();
    waveletData.createDocument("map+MAP0002", creator, Collections.singleton(creator), content,
        System.currentTimeMillis(), 0);


    content = BasicFactories.documentProvider().parse(LIST_02_DOC_CONTENT).toInitialization();
    waveletData.createDocument("list+LIST0002", creator, Collections.singleton(creator), content,
        System.currentTimeMillis(), 0);
  }

  protected WaveletData getWaveletData() {
    return waveletData;
  }

}
