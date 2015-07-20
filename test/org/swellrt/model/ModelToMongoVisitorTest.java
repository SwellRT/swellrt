package org.swellrt.model;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import org.swellrt.model.unmutable.UnmutableModel;


public class ModelToMongoVisitorTest extends WaveletBasedAbstractTest {

  protected void setUp() throws Exception {
    super.setUp();
  }


  public void testVisitor() {

    ReadableModel model = UnmutableModel.create(getWaveletData());

    BasicDBObject mongoModel = ModelToMongoVisitor.getDBObject(model);

    assertEquals("example.com/w+seedA", mongoModel.get("wave_id"));
    assertEquals("example.com/conv+seedB", mongoModel.get("wavelet_id"));

    assertEquals("tom@example.com", ((BasicDBList) mongoModel.get("participants")).get(0));
    assertEquals("tim@example.com", ((BasicDBList) mongoModel.get("participants")).get(1));

    BasicDBObject root = (BasicDBObject) mongoModel.get("root");

    assertEquals("This is the string 0", root.get("keystring"));

    BasicDBObject map = (BasicDBObject) root.get("keymap");

    assertEquals("This is the string 1", map.get("keyone"));
    assertEquals("This is the string 2", map.get("keytwo"));

    BasicDBList list = (BasicDBList) root.get("keylist");

    assertEquals("This is the string 4", ((BasicDBObject) list.get(0)).get("keyone"));
    assertEquals("This is the string 5", ((BasicDBList) list.get(1)).get(0));
    assertEquals("This is the string 3", list.get(2));

  }

}
