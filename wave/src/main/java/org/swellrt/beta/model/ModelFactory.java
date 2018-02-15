package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.wave.Blip;

public abstract class ModelFactory {

  /**
   * Default is Java Implementation. TODO Avoid this global dependency
   */
  public static ModelFactory instance = null;

  /**
   * Creates a text node supported by a Wave document.
   * <p>
   *
   * @param nodeManager
   *          the node manager of the Swell
   * @param substrateId
   *          the id of the document in the Wave/Object
   * @param blip
   *          the wavelet's blip view of the document
   * @param docInit
   *          initialization data if blip is new
   * @param doc
   *          the interactive view of the document
   *
   * @return
   */
  public abstract SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
      Blip blip, DocInitialization docInit,
      InteractiveDocument doc);

  /**
   * Creates a stand alone text node.
   *
   * @param text
   * @return
   * @throws SException
   */
  public abstract SText createLocalText(String text) throws SException;

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
