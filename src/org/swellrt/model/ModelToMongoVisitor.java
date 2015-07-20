package org.swellrt.model;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import org.waveprotocol.wave.federation.xmpp.Base64Util;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Stack;


public class ModelToMongoVisitor implements TypeVisitor {


  private BasicDBObject document;
  private Stack<Object> objects;

  public static BasicDBObject getDBObject(ReadableModel model) {

    ModelToMongoVisitor visitor = new ModelToMongoVisitor();
    visitor.visit(model);
    return visitor.getDBObject();
  }

  protected ModelToMongoVisitor() {
    this.document = new BasicDBObject();
    this.objects = new Stack<Object>();
  }

  protected BasicDBObject getDBObject() {
    return document;
  }

  @Override
  public void visit(ReadableModel instance) {

    document.append("wave_id", instance.getWaveId());
    document.append("wavelet_id", instance.getWaveletId());

    // Add participants
    BasicDBList participants = new BasicDBList();
    for (ParticipantId p : instance.getParticipants()) {
      participants.add(p.getAddress());
    }
    document.append("participants", participants);

    // Root map
    instance.getRoot().accept(this);
    document.put("root", objects.pop());


  }

  @Override
  public void visit(ReadableString instance) {
    this.objects.add(instance.getValue());

  }

  @Override
  public void visit(ReadableMap instance) {

    BasicDBObject mapDBObject = new BasicDBObject();
    for (String k : instance.keySet()) {
      instance.get(k).accept(this);
      mapDBObject.put(k, objects.pop());
    }
    objects.push(mapDBObject);


  }

  @Override
  public void visit(ReadableList instance) {

    BasicDBList listDBObject = new BasicDBList();
    for (ReadableType t : instance.getValues()) {
      t.accept(this);
      listDBObject.add(objects.pop());
    }
    objects.push(listDBObject);

  }

  @Override
  public void visit(ReadableText instance) {

    // TODO (pablojan) serialize annotations
    BasicDBObject textDBObject = new BasicDBObject();
    textDBObject.append("annotations", "");
    // TODO (pablojan) add test case for Text objects
    textDBObject.append("xml", Base64Util.encode(instance.getXml().getBytes()));
    objects.push(textDBObject);

  }

}
