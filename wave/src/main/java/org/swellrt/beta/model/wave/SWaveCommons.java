package org.swellrt.beta.model.wave;

import java.util.Map;

import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Preconditions;

public class SWaveCommons {


  protected static final String MASTER_DATA_WAVELET_NAME = "data+master";
  protected static final String CONTAINER_DATA_WAVELET_PREFIX = "data+";
  protected static final String ROOT_SUBSTRATE_ID = "m+root";

  protected static final String MAP_TAG = "map";
  protected static final String MAP_ENTRY_TAG = "entry";
  protected static final String MAP_ENTRY_KEY_ATTR = "k";
  protected static final String MAP_ENTRY_VALUE_ATTR = "v";

  protected static final String LIST_TAG = "list";
  protected static final String LIST_ENTRY_TAG = "entry";
  protected static final String LIST_ENTRY_KEY_ATTR = "k";
  protected static final String LIST_ENTRY_VALUE_ATTR = "v";

  protected static final String USER_ROOT_SUBSTRATED_ID = "m+root";

  public static abstract class Deserializer {

    SWaveNode deserialize(String s) {

      Preconditions.checkNotNull(s, "Unable to deserialize a null value");

      SubstrateId substrateId = SubstrateId.deserialize(s);
      if (substrateId != null) {

        if (substrateId.isList())
          return materializeList(substrateId);

        if (substrateId.isMap())
          return materializeMap(substrateId);

        if (substrateId.isText())
          return materializeText(substrateId, null);

        return null;

      } else {
        return SPrimitive.deserialize(s);
      }

    }

    protected abstract SWaveNode materializeList(SubstrateId substrateId);

    protected abstract SWaveNode materializeMap(SubstrateId substrateId);

    protected abstract SWaveNode materializeText(SubstrateId substrateId, DocInitialization docInit);



  }

  /**
   * Serialize a SNode to be stored in a Swell blip.
   *
   * @param x
   * @return
   */
  public static String serialize(SNode x) {

    // Order matters check SPrimitive first
    if (x instanceof SPrimitive) {
      SPrimitive p = (SPrimitive) x;
      return p.serialize();
    }

    if (x instanceof SWaveNode) {
      SWaveNode r = (SWaveNode) x;
      SubstrateId id = r.getSubstrateId();
      if (id != null)
        return id.serialize();
    }

    return null;
  }

  /**
   * A serializer/deserializer of SNode objects to/from a Wave's list
   */
  public static class SubstrateListSerializer
      implements org.waveprotocol.wave.model.adt.docbased.Factory<Doc.E, SWaveNode, SWaveNode> {

    Deserializer d;

    public SubstrateListSerializer(Deserializer d) {
      this.d = d;
    }

    @Override
    public SWaveNode adapt(DocumentEventRouter<? super E, E, ?> router, E element) {
      Map<String, String> attributes = router.getDocument().getAttributes(element);
      return d.deserialize(attributes.get(LIST_ENTRY_VALUE_ATTR));
    }

    @Override
    public Initializer createInitializer(SWaveNode node) {
      return new org.waveprotocol.wave.model.adt.docbased.Initializer() {

        @Override
        public void initialize(Map<String, String> target) {
          target.put(LIST_ENTRY_KEY_ATTR, String.valueOf(System.currentTimeMillis())); // temp
          target.put(LIST_ENTRY_VALUE_ATTR, serialize(node));
        }

      };
    }

  }

  /**
   * A serializer/deserializer of SNode objects to/from a Wave's map
   */
  public static class SubstrateMapSerializer
      implements org.waveprotocol.wave.model.util.Serializer<SWaveNode> {

    Deserializer d;

    public SubstrateMapSerializer(Deserializer d) {
      this.d = d;
    }

    @Override
    public String toString(SWaveNode x) {
      return serialize(x);
    }

    @Override
    public SWaveNode fromString(String s) {
      return d.deserialize(s);
    }

    @Override
    public SWaveNode fromString(String s, SWaveNode defaultValue) {
      return fromString(s);
    }

  }

}
