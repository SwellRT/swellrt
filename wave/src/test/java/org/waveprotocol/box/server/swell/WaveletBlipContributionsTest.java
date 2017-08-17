package org.waveprotocol.box.server.swell;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.waveprotocol.box.server.swell.WaveletContributions.BlipContributions;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.indexed.SimpleAnnotationSet;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.impl.PluggableMutableDocument;

import com.google.common.collect.Range;

import junit.framework.TestCase;

public class WaveletBlipContributionsTest extends TestCase {

  static final String BLIP_ID = "t+dummy";
  static final WaveletId WAVELET_ID = WaveletId.of("local.net", "dummy");

  ParticipantId alice, bob, creator;
  BasicWaveletOperationContextFactory opContextFactoryAlice, opContextFactoryBob, currentOpContextFactory;
  PluggableMutableDocument document;
  WaveletContributions contributionsManager;



  @Override
  protected void setUp() throws Exception {
    super.setUp();

   contributionsManager = new WaveletContributions(WaveletName.of("local.net", "waveid", "local.net", "waveletid"));
   creator = ParticipantId.ofUnsafe("creator@local.net");

   alice = ParticipantId.ofUnsafe("alice@local.net");
   opContextFactoryAlice = new BasicWaveletOperationContextFactory(alice);

   bob = ParticipantId.ofUnsafe("bob@local.net");
   opContextFactoryBob = new BasicWaveletOperationContextFactory(bob);

   DocInitialization docInit = BasicFactories.documentProvider().parse("<body>Lorem ipsum dolor sit amet, consectetur adipiscing elit</body>").toInitialization();
   document = BasicFactories.pluggableMutableDocumentFactory().create(WAVELET_ID, BLIP_ID, docInit);
   document.init(new SilentOperationSink<DocOp> () {

    @Override
    public void consume(DocOp op) {
      if (currentOpContextFactory == null) {
        throw new RuntimeException("No operation context factory is set!");
      }
      BlipOperation blipOp = new BlipContentOperation(currentOpContextFactory.createContext(), op);
      contributionsManager.apply(new WaveletBlipOperation(BLIP_ID, blipOp));
    }

   });

   contributionsManager.init(BLIP_ID, docInit, creator);
  }


  protected Map<ParticipantId, Set<Range<Integer>>> getAndPrintAnnotations(PluggableMutableDocument document, SimpleAnnotationSet annotationSet) {

    Map<ParticipantId, Set<Range<Integer>>> rangesPerParticipant = new HashMap<ParticipantId, Set<Range<Integer>>>();

    Iterable<AnnotationInterval<Object>> intervals =
    annotationSet.annotationIntervals(0, document.size(), CollectionUtils.newStringSet(WaveletContributions.ANNOTATION_KEY));
    intervals.forEach(interval -> {
      System.out.println("Interval ("+interval.start()+","+interval.end()+") ");
      final Range<Integer> r = Range.closed(interval.start(), interval.end());
      interval.annotations().each(new ProcV<Object>() {

        @Override
        public void apply(String key, Object value) {

          if (value != null) {
            ParticipantId p = (ParticipantId) value;
            if (!rangesPerParticipant.containsKey(p)) {
              rangesPerParticipant.put(p, new HashSet<Range<Integer>>());
            }
            Set<Range<Integer>> rangeSet = rangesPerParticipant.get(p);
            rangeSet.add(r);

            System.out.println("\t "+key+"="+value.toString());
          }

        }

      });
    });

    return rangesPerParticipant;
  }

  public void testInsert() {

    currentOpContextFactory = opContextFactoryAlice;
    document.insertText(6, " Hello");

    currentOpContextFactory = opContextFactoryBob;
    document.insertText(12, " world");

    BlipContributions blipContribs = contributionsManager.blipContribsMap.get(BLIP_ID);
    assertNotNull(blipContribs);

    System.out.println(document.toXmlString());
    Map<ParticipantId, Set<Range<Integer>>> ranges = getAndPrintAnnotations(document, blipContribs.annotations);

    assertTrue(ranges.get(creator).contains(Range.closed(0, 6)));
    assertTrue(ranges.get(creator).contains(Range.closed(18, 69)));

    assertTrue(ranges.get(alice).contains(Range.closed(6, 12)));
    assertTrue(ranges.get(bob).contains(Range.closed(12, 18)));

  }



  public void testDeleteInsert() {

    currentOpContextFactory = opContextFactoryAlice;
    document.deleteRange(7, 12);

    currentOpContextFactory = opContextFactoryBob;
    document.insertText(7, "DELETE");

    BlipContributions blipContribs = contributionsManager.blipContribsMap.get(BLIP_ID);
    assertNotNull(blipContribs);

    System.out.println(document.toXmlString());
    Map<ParticipantId, Set<Range<Integer>>> ranges = getAndPrintAnnotations(document, blipContribs.annotations);

    assertTrue(ranges.get(creator).contains(Range.closed(0, 7)));
    assertTrue(ranges.get(creator).contains(Range.closed(13, 58)));

    assertTrue(ranges.get(bob).contains(Range.closed(7, 13)));

  }


  public void testXmlBlockInsert() {

    currentOpContextFactory = opContextFactoryAlice;
    Doc.E body = DocHelper.getFirstChildElement(document, document.getDocumentElement());
    Doc.N e = document.getFirstChild(body);
    Point<N> p = Point.inText(e, 6);
    document.createElement(p, "line", Collections.emptyMap());

    BlipContributions blipContribs = contributionsManager.blipContribsMap.get(BLIP_ID);
    assertNotNull(blipContribs);

    System.out.println(document.toXmlString());
    Map<ParticipantId, Set<Range<Integer>>> ranges = getAndPrintAnnotations(document, blipContribs.annotations);

    assertTrue(ranges.get(creator).contains(Range.closed(0, 7)));
    assertTrue(ranges.get(creator).contains(Range.closed(9, 59)));

    assertTrue(ranges.get(alice).contains(Range.closed(7, 9)));

  }

}
