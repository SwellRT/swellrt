package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.waveprotocol.wave.client.common.util.ClientPercentEncoderDecoder;
import org.waveprotocol.wave.client.editor.playback.DocHistoryFake.Delta;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;

/**
 * A utility class to build a document and its history by adding lines of text.
 */
public class DocHistoryFakeBuilder {

  private byte[] fakeDeltaBytes = new byte[] { 1, 1 };

  private WaveletName waveletName = WaveletName.of("local.net", "dummywave", "local.net",
      "dummywavelet");

  private HashedVersionFactory hashFactory = new HashedVersionFactoryImpl(
      new IdURIEncoderDecoder(new ClientPercentEncoderDecoder()));

  List<Delta> deltaHistory = new ArrayList<Delta>();

  private int docIndex = 1;

  private String participant;

  public List<DocOp> ops = new ArrayList<DocOp>();

  private OperationSequencer<Nindo> opSequencerRecorder = new OperationSequencer<Nindo>() {

    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }

    @Override
    public void consume(Nindo op) {
      try {
        ops.add(indexedDocument.consumeAndReturnInvertible(op));
      } catch (OperationException e) {
        throw new IllegalStateException(e);
      }
    }

  };
  private IndexedDocument<Node, Element, Text> indexedDocument = DocProviders.POJO
      .parse("<body></body>");
  private MutableDocument<Node, Element, Text> document = new MutableDocumentImpl<Node, Element, Text>(
      opSequencerRecorder, indexedDocument);

  public DocHistoryFakeBuilder() {
  }

  private void createDelta() {
    Delta delta = new DocHistoryFake.Delta();
    delta.timestap = System.currentTimeMillis();
    delta.participant = participant;
    delta.ops = ops.toArray(new DocOp[ops.size()]);

    if (deltaHistory.isEmpty())
      delta.appliedToVersion = hashFactory.createVersionZero(waveletName);
    else
      delta.appliedToVersion = deltaHistory.get(deltaHistory.size() - 1).resultingVersion;

    delta.resultingVersion = hashFactory.create(fakeDeltaBytes, delta.appliedToVersion, ops.size());

    deltaHistory.add(delta);
  }

  public void appendLineAsDelta(String line, String participant) {
    this.participant = participant;

    ops.clear(); // start new delta

    if (deltaHistory.isEmpty()) {
      // add document's <body></body> tags as content of first delta
      ops.add(indexedDocument.asOperation());
    }

    document.insertXml(document.locate(docIndex),
        XmlStringBuilder.createFromXmlString("<line></line>"));
    docIndex += 2;

    String[] words = line.split(" ");
    for (int i = 0; i < words.length; i++) {
      String s = words[i] + " ";
      document.insertText(docIndex, s);
      docIndex += s.length();
    }


    createDelta();
  }

  public Delta getLastDelta() {
    return deltaHistory.get(deltaHistory.size() - 1);
  }

  public List<Delta> getDeltas() {
    return deltaHistory;
  }

  protected static void traverseAllPrev(DocHistory.Iterator it, Consumer<DocRevision> consumer) {
    it.prev(r -> {
      if (r != null) {
        consumer.accept(r);
        traverseAllPrev(it, consumer);
      }
    });
  }

  protected static void traverseAllNext(DocHistory.Iterator it, Consumer<DocRevision> consumer) {
    it.next(r -> {
      if (r != null) {
        consumer.accept(r);
        traverseAllNext(it, consumer);
      }
    });
  }

  public static void main(String[] args) {

    DocHistoryFakeBuilder db = new DocHistoryFakeBuilder();

    // revision #0

    db.appendLineAsDelta("The opening ceremony of any Olympics provides pageantry at a global scale", "ann@local.net");
    db.appendLineAsDelta("a celebration that, at its best, can create moments every bit as indelible as the games themselves", "ann@local.net");
    db.appendLineAsDelta("In Pyeongchang, the curtain-raiser also includes a site never seen before", "ann@local.net");
    db.appendLineAsDelta("a record-setting 1,218 drones joined in a mechanical murmuration.", "ann@local.net");

    // revision #1

    db.appendLineAsDelta("Drone shows like the one on display at the Pyeongchang Games have taken place before,", "bob@local.net");
    db.appendLineAsDelta("you may remember the drone army that flanked Lady Gaga at last year's Super Bowl.", "bob@local.net");
    db.appendLineAsDelta("But the burst of drones that filled the sky Friday night -or early morning", "bob@local.net");
    db.appendLineAsDelta("depending on where in the world you watched-", "bob@local.net");
    db.appendLineAsDelta("comprised four times as many fliers.", "bob@local.net");
    db.appendLineAsDelta("Without hyperbole, there's really never been anything like it.",
        "bob@local.net");

    System.out.println(db.document.toXmlString());
    System.out.println("Deltas generated = " + db.deltaHistory.size());

    // DocHistory

    DocHistory history = new DocHistoryFake(db.deltaHistory);


    DocHistory.Iterator historyIterator = history.getIterator();

    traverseAllPrev(historyIterator, System.out::println);

    historyIterator.current(revision -> {
      history.getSnapshot(revision, snapshot -> {
        System.out.println(revision);
        System.out.println(DocOpUtil.toXmlString(snapshot));
      });
    });


    traverseAllNext(historyIterator, revision -> {
      history.getSnapshot(revision, snapshot -> {
        System.out.println(revision);
        System.out.println(DocOpUtil.toXmlString(snapshot));
      });
    });

  }

}
