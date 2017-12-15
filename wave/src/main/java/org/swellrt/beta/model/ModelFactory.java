package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.wave.Blip;

public abstract class ModelFactory {

  /**
   * Default is Java Implementation. TODO Avoid this global dependency
   */
  public static ModelFactory instance = null;

  public abstract SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
      Blip blip,
      InteractiveDocument doc);

  public abstract STextLocal createLocalText(String text) throws SException;

  /** Check if an object's type is a valid Json representation:
   *  <li>JavaScriptObject for GWT</li>
   *  <li>JsonObject (Gson) for Java</li>
   */
  public abstract boolean isJsonObject(Object o);

  /** Parse/Deserialize a Json object */
  public abstract Object parseJsonObject(String json);

  /** Serialize a Json object */
  public abstract String serializeJsonObject(Object o);

  /** Return a property value in the Json object, as primitive or Json object */
  public abstract Object traverseJsonObject(Object o, String path);

  /** Translate JSON tree to SNode tree */
  public abstract SNodeBuilder getSNodeBuilder();

  /** Translate SNode tree to JSON tree */
  public abstract SViewBuilder getJsonBuilder(SNode node);

}
