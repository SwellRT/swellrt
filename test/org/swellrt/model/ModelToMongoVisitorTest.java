package org.swellrt.model;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import org.swellrt.model.unmutable.UnmutableModel;
import org.swellrt.server.box.ModelIndexerVisitor;
import org.waveprotocol.wave.model.util.Pair;

import java.util.Map;


public class ModelToMongoVisitorTest extends WaveletBasedTestBase {

  protected void setUp() throws Exception {
    super.setUp();
  }


  public void testVisitor() {

    ReadableModel model = UnmutableModel.create(getWaveletData());

    Pair<BasicDBObject, Map<String, String>> visitorResult = ModelIndexerVisitor.run(model);

    BasicDBObject mongoModel = visitorResult.first;

    assertEquals("example.com/w+seedA", mongoModel.get("wave_id"));
    assertEquals("example.com/conv+seedB", mongoModel.get("wavelet_id"));

    assertEquals("tom@example.com", ((BasicDBList) mongoModel.get("participants")).get(0));
    assertEquals("tim@example.com", ((BasicDBList) mongoModel.get("participants")).get(1));

    BasicDBObject root = (BasicDBObject) mongoModel.get("root");

    assertEquals("This is string 0", root.get("key0"));
    assertEquals("This is string 1", root.get("key3"));

    BasicDBObject map = (BasicDBObject) root.get("key1");

    assertEquals("This is string 2", map.get("key10"));

    BasicDBList list = (BasicDBList) root.get("key2");

    assertEquals("This is string 4", list.get(0));
    assertEquals("This is string 5", list.get(1));

  }

}
