package org.swellrt.beta.model.wave;

import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Class representing a SNode substrate id.
 * <p>
 * A substrate has three elements:
 * <ul>
 * <li>type of the container node</li>
 * <li>document id of this node substrate</li>
 * <li>wavelet id where the document is stored</li>
 * </ul>
 * <p>
 * Example: map => m:data+34js1a:m+3jg39s
 * <p>
 * The document id must contain also the type prefix
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SubstrateId {

  private static final String SEPARATOR = ":";
  private static final String MAP_TYPE_PREFIX = "m";
  private static final String LIST_TYPE_PREFIX = "l";
  private static final String TEXT_TYPE_PREFIX = "t";
  private static final String TOKEN_SEPARATOR = "+";


  private final String type;
  private final WaveletId containerId;
  private final String documentId;

  private final String str;


  public static boolean isMap(SubstrateId id) {
    Preconditions.checkArgument(id != null, "Null substrate id");
    return MAP_TYPE_PREFIX.equals(id.type);
  }

  public static boolean isText(SubstrateId id) {
    Preconditions.checkArgument(id != null, "Null substrate id");
    return TEXT_TYPE_PREFIX.equals(id.type);
  }

  public static boolean isText(String documdentId) {
    Preconditions.checkArgument(documdentId != null, "Null document id");
    return documdentId.startsWith(TEXT_TYPE_PREFIX+TOKEN_SEPARATOR);
  }

  private static SubstrateId of(String type, WaveletId containerId, String substrateId) {
    Preconditions.checkArgument(containerId != null, "Null container id");
    Preconditions.checkArgument(substrateId != null, "Null substrate id");
    return new SubstrateId(type, containerId, substrateId);
  }

  public static SubstrateId ofMap(WaveletId containerId, String substrateId) {
    Preconditions.checkArgument(substrateId.startsWith(MAP_TYPE_PREFIX+TOKEN_SEPARATOR), "Bad substrate id format");
    return of(MAP_TYPE_PREFIX, containerId, substrateId);
  }

  public static SubstrateId ofList(WaveletId containerId, String substrateId) {
    Preconditions.checkArgument(substrateId.startsWith(LIST_TYPE_PREFIX+TOKEN_SEPARATOR), "Bad substrate id format");
    return of(LIST_TYPE_PREFIX, containerId, substrateId);
  }

  public static SubstrateId ofText(WaveletId containerId, String substrateId) {
    Preconditions.checkArgument(substrateId.startsWith(TEXT_TYPE_PREFIX+TOKEN_SEPARATOR), "Bad substrate id format");
    return of(TEXT_TYPE_PREFIX, containerId, substrateId);
  }

  public static SubstrateId deserialize(String s) {
      Preconditions.checkArgument(s != null && !s.isEmpty(), "String is null or empty");

      String parts[] = s.split(SEPARATOR);
      if (parts.length != 3)
        return null;


      if (!parts[0].equals(MAP_TYPE_PREFIX) &&
          !parts[0].equals(LIST_TYPE_PREFIX) &&
          !parts[0].equals(TEXT_TYPE_PREFIX))
        return null;

      if (!parts[2].startsWith(MAP_TYPE_PREFIX+TOKEN_SEPARATOR) &&
          !parts[2].startsWith(LIST_TYPE_PREFIX+TOKEN_SEPARATOR) &&
          !parts[2].startsWith(TEXT_TYPE_PREFIX+TOKEN_SEPARATOR))
        return null;

      WaveletId containerId = null;
      try {
        containerId = ModernIdSerialiser.INSTANCE.deserialiseWaveletId(parts[1]);
      } catch (InvalidIdException e) {

      }

      if (containerId == null)
        return null;

      return SubstrateId.of(parts[0], containerId, parts[2]);
  }

  public static SubstrateId createForMap(WaveletId containerId, IdGenerator tokenGenerator) {
    return of(MAP_TYPE_PREFIX, containerId, MAP_TYPE_PREFIX+TOKEN_SEPARATOR+tokenGenerator.newUniqueToken());
  }

  public static SubstrateId createForList(WaveletId containerId, IdGenerator tokenGenerator) {
    return of(LIST_TYPE_PREFIX, containerId, LIST_TYPE_PREFIX+TOKEN_SEPARATOR+tokenGenerator.newUniqueToken());
  }

  public static SubstrateId createForText(WaveletId containerId, IdGenerator tokenGenerator) {
    return of(TEXT_TYPE_PREFIX, containerId, TEXT_TYPE_PREFIX+TOKEN_SEPARATOR+tokenGenerator.newUniqueToken());
  }

  protected SubstrateId(String type, WaveletId containerId, String substrateId) {
    this.type = type;
    this.containerId = containerId;
    this.documentId = substrateId;
    this.str = type+SEPARATOR+ModernIdSerialiser.INSTANCE.serialiseWaveletId(containerId)+SEPARATOR+substrateId;

  }

  public boolean isList() {
    return LIST_TYPE_PREFIX.equals(type);
  }

  public boolean isMap() {
    return MAP_TYPE_PREFIX.equals(type);
  }

  public boolean isText() {
    return TEXT_TYPE_PREFIX.equals(type);
  }

  public String getType() {
    return type;
  }


  public WaveletId getContainerId() {
    return containerId;
  }


  public String getDocumentId() {
    return documentId;
  }

  public String serialize() {
    return str;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((containerId == null) ? 0 : containerId.hashCode());
    result = prime * result + ((documentId == null) ? 0 : documentId.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SubstrateId other = (SubstrateId) obj;
    if (containerId == null) {
      if (other.containerId != null)
        return false;
    } else if (!containerId.equals(other.containerId))
      return false;
    if (documentId == null) {
      if (other.documentId != null)
        return false;
    } else if (!documentId.equals(other.documentId))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }



}
