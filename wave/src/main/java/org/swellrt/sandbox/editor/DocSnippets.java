package org.swellrt.sandbox.editor;

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.client.concurrencycontrol.ProxyOperationSink;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.wave.data.impl.GroupOperationSequencer;

public class DocSnippets {

  public static void console(String s) {
    System.out.println(s);
  }

  public static void title(String title) {
    System.out.println("--------------------- " + title + " ---------------------");
    System.out.println();
  }

  public static void line() {
    System.out.println();
  }

  public static void console(String title, String s) {
    System.out.println("--------------------- " + title + " ---------------------");
    System.out.println();
    System.out.println(s);
    System.out.println();
  }

  public static void console(String title, MutableDocument doc) {
    System.out.println("--------------------- " + title + " ---------------------");
    System.out.println();
    System.out.println(DocOpUtil.debugToXmlString(doc.toInitialization()));
    System.out.println();
  }


  public static void main(String... args) {


    /*

      MutableDocument = IndexedDocument + OperationSequencer

      - MutableDocument has a char position based interface to do changes in XML. insertText()...

      - IndexedDocument has a DOM-style interface, is read only. IndexedDocuments works on top of RawDocuments

      - OperationSequencer is an ops sink that can do batch processing (groups...)


      Mutation operations in MutableDocument...
        - read the current state from IndexedDocument
        - build doc ops representing the mutation
        - send the ops to the operation sequencer

      The operation sequencer consume ops (created in the mutable doc) and...
        - pass ops to the IndexedDocument to be consumed ( consume() or consumeAndReturnInvertible() )
        - sends out the operation to an output sink

    */

    //
    // Source Doc
    //

    String text = "";
    IndexedDocument<Node, Element, Text> sourceIndexedDoc = DocProviders.POJO.parse(text);
    ProxyOperationSink<DocOp> proxyOutputSink = ProxyOperationSink.<DocOp> create();
    OperationSequencer<Nindo> groupSequencer = new GroupOperationSequencer(sourceIndexedDoc, proxyOutputSink);
    MutableDocument<Node, Element, Text> sourceDoc = new MutableDocumentImpl<Node, Element, Text>(
        groupSequencer, sourceIndexedDoc);

    //
    // Target Doc
    //
    IndexedDocument<Node, Element, Text> targetIndexedDoc = DocProviders.POJO.parse("");
    MutableDocument<Node, Element, Text> targetDoc = new MutableDocumentImpl<Node, Element, Text>(
        new OperationSequencer<Nindo>() {

          @Override
          public void begin() {
          }

          @Override
          public void end() {
          }

          @Override
          public void consume(Nindo op) {
            try {
              targetIndexedDoc.consumeAndReturnInvertible(op);
            } catch (OperationException e) {
              e.printStackTrace();
            }

          }
        }, targetIndexedDoc);

    /*

      to remind, some helper classes

        DocOpUtil

        DocProviders
        DocHelper
        DocCompare
        DomOperationUtil

        XmlStringBuilder
        XmlStringBuilderDoc

     */



    //
    // Adding doc ops manually
    //

    final List<DocOp> opQueue = new ArrayList<DocOp>();

    proxyOutputSink.setTarget(new SilentOperationSink<DocOp>() {

      @Override
      public void consume(DocOp op) {
        opQueue.add(op);
      }

    });

    sourceDoc.beginMutationGroup();
    sourceDoc.insertText(0, " Something new");
    sourceDoc.endMutationGroup();

    console("init doc content",
        DocOpUtil.debugToXmlString(sourceDoc.toInitialization()));

    // generate ops editing a doc, replay them in another one

    sourceDoc.beginMutationGroup();
    sourceDoc.insertText(11, "more than ");
    sourceDoc.endMutationGroup();

    console("mutation", sourceDoc);

    sourceDoc.beginMutationGroup();
    sourceDoc.deleteRange(21, sourceDoc.size());
    sourceDoc.endMutationGroup();

    console("mutation", sourceDoc);

    sourceDoc.beginMutationGroup();
    sourceDoc.insertText(sourceDoc.size() - 1, " anything better.");
    sourceDoc.endMutationGroup();

    title("doc ops");

    opQueue.forEach(op -> {
      console(DocOpUtil.toConciseString(op));
    });

    line();

    targetDoc.hackConsume(Nindo.fromDocOp(opQueue.get(0), true));
    targetDoc.hackConsume(Nindo.fromDocOp(opQueue.get(1), true));

    console("play >> 2", targetDoc);

    targetDoc.hackConsume(Nindo.fromDocOp(opQueue.get(2), true));

    console("play >> 3", targetDoc);


    targetDoc.hackConsume(Nindo.fromDocOp(DocOpInverter.invert(opQueue.get(2)), true));
    console("rewind 2 <<", targetDoc);


    targetDoc.hackConsume(Nindo.fromDocOp(DocOpInverter.invert(opQueue.get(1)), true));
    console("rewind 1 <<", targetDoc);

  }


}
