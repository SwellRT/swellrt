package org.swellrt.server.box.index;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import org.swellrt.model.ReadableFile;
import org.swellrt.model.ReadableList;
import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableModel;
import org.swellrt.model.ReadableString;
import org.swellrt.model.ReadableText;
import org.swellrt.model.ReadableType;
import org.swellrt.model.ReadableTypeVisitor;
import org.swellrt.model.shared.ModelUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


/**
 * Build a MongoDB document with the snapshot of a collaborative data model.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ModelIndexerVisitor implements ReadableTypeVisitor {


  private final BasicDBObject document;
  private final Stack<Object> objects;
  protected final Stack<String> path;
  protected final Map<String, String> blipIdToPathMap;

  /**
   * Generate a BSON view of the Wave-based Data Model. Also return a map
   * between collaborative data model objects' paths to the corresponding
   * blip/document storing it.
   *
   * @param model the Wave-based collaborative data model
   * @return
   */
  public static Pair<BasicDBObject, Map<String, String>> run(ReadableModel model) {

    ModelIndexerVisitor visitor = new ModelIndexerVisitor();
    visitor.visit(model);
    return Pair.<BasicDBObject, Map<String, String>> of(visitor.getDBObject(),
        visitor.getblipIdToPathMap());
  }

  protected ModelIndexerVisitor() {
    this.document = new BasicDBObject();
    this.objects = new Stack<Object>();
    this.path = new Stack<String>();
    this.blipIdToPathMap = new HashMap<String, String>();
  }

  protected BasicDBObject getDBObject() {
    return document;
  }

  protected Map<String, String> getblipIdToPathMap() {
    return blipIdToPathMap;
  }


  @Override
  public void visit(ReadableModel instance) {

    document.append("wave_id", ModelUtils.serialize(instance.getWaveId()));
    document.append("wavelet_id", ModelUtils.serialize(instance.getWaveletId()));

    // Add participants
    BasicDBList participants = new BasicDBList();
    for (ParticipantId p : instance.getParticipants()) {
      participants.add(p.getAddress());
    }
    document.append("participants", participants);

    // Root map
    path.push("root");
    instance.getRoot().accept(this);
    document.put("root", objects.pop());
    path.pop();


  }

  @Override
  public void visit(ReadableString instance) {
    this.objects.add(instance.getValue());

  }

  @Override
  public void visit(ReadableMap instance) {

    BasicDBObject mapDBObject = new BasicDBObject();
    for (String k : instance.keySet()) {
      path.push(k);

      // Avoid issues on non initialized blips (blips with no content)
      ReadableType t = instance.get(k);
      if (t != null) {
        instance.get(k).accept(this);
        mapDBObject.put(k, objects.pop());
      }

      path.pop();
    }
    objects.push(mapDBObject);


  }

  @Override
  public void visit(ReadableList<? extends ReadableType> instance) {
    // TODO(pablojan) add getDocumentedId to ReadableList
    // pathToBlipMap.put(getStringPath(), instance.)

    BasicDBList listDBObject = new BasicDBList();
    int i = 0;
    for (ReadableType t : instance.getValues()) {
      path.push("" + (i++));
      t.accept(this);
      listDBObject.add(objects.pop());
      path.pop();
    }
    objects.push(listDBObject);

  }

  @Override
  public void visit(ReadableText instance) {

    blipIdToPathMap.put(instance.getDocumentId(), getStringPath());

    // TODO (pablojan) serialize annotations
    BasicDBObject textDBObject = new BasicDBObject();
    textDBObject.append("annotations", "");
    // TODO (pablojan) add test case for Text objects
    textDBObject.append("xml", instance.getXml());

    textDBObject.append("author", instance.getAuthor().getAddress());
    textDBObject.append("contributors", toDBList(instance.getContributors()));
    textDBObject.append("lastmodtime", instance.getLastUpdateTime());

    objects.push(textDBObject);

  }


  @Override
  public void visit(ReadableFile instance) {
    // TODO Auto-generated method stub

  }


  protected String getStringPath() {
    String strPath = "";
    for (String e : path) {
      if (!strPath.isEmpty()) strPath += ".";

      strPath += e;
    }

    return strPath;
  }

  protected BasicDBList toDBList(Set<ParticipantId> participantSet) {
    BasicDBList listDBObject = new BasicDBList();
    for (ParticipantId p : participantSet)
      listDBObject.add(p.getAddress());

    return listDBObject;
  }


}
