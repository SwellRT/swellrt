package org.swellrt.beta.model.wave.unmutable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.WaveCommons;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import com.google.gson.JsonParser;

public class UnmutableWaveNodeManager {


  /*
   * Be careful. This constants are duplicated in {@link SPrimitive}
   */
  private static final String PRIMITIVE_SEPARATOR = ":";
  private static final String PRIMITIVE_STRING_TYPE_PREFIX = "s";
  private static final String PRIMITIVE_BOOLEAN_TYPE_PREFIX = "b";
  private static final String PRIMITIVE_INTEGER_TYPE_PREFIX = "i";
  private static final String PRIMITIVE_DOUBLE_TYPE_PREFIX = "d";
  private static final String PRIMITIVE_JSO_TYPE_PREFIX = "js";

  private final WaveletProvider waveletProvider;
  private final WaveId waveId;

  public UnmutableWaveNodeManager(WaveId waveId, WaveletProvider waveletProvider) {
    super();
    this.waveId = waveId;
    this.waveletProvider = waveletProvider;
  }

  public WaveletId getMasterWaveletId() {
    return WaveletId.of(waveId.getDomain(), WaveCommons.MASTER_DATA_WAVELET_NAME);
  }

  protected ReadableWaveletData getWaveletData(SubstrateId substrateId) {

    WaveletName waveletName = WaveletName.of(waveId, substrateId.getContainerId());
    CommittedWaveletSnapshot snapshot;
    try {
      snapshot = waveletProvider.getSnapshot(waveletName);
    } catch (WaveServerException e) {
      e.printStackTrace();
      return null;
    }
    if (snapshot == null)
      throw new IllegalArgumentException("Unknown wavelet id");

    return snapshot.snapshot;
  }

  protected Document getDocument(SubstrateId substrateId) {
    ReadableWaveletData waveletData = getWaveletData(substrateId);
    return waveletData.getDocument(substrateId.getDocumentId()).getContent().getMutableDocument();
  }

  private UnmutablePrimitive deserializePrimitive(String rawValue) {

    Object value = null;

    if (rawValue.startsWith(PRIMITIVE_STRING_TYPE_PREFIX + PRIMITIVE_SEPARATOR)) {
      value = rawValue.substring(2);
    }

    if (rawValue.startsWith(PRIMITIVE_INTEGER_TYPE_PREFIX + PRIMITIVE_SEPARATOR)) {
      try {
        value = Integer.parseInt(rawValue.substring(2));
      } catch (NumberFormatException e) {

      }
    }

    if (rawValue.startsWith(PRIMITIVE_DOUBLE_TYPE_PREFIX + PRIMITIVE_SEPARATOR)) {
      try {
        value = Double.parseDouble(rawValue.substring(2));
      } catch (NumberFormatException e) {

      }
    }

    if (rawValue.startsWith(PRIMITIVE_BOOLEAN_TYPE_PREFIX + PRIMITIVE_SEPARATOR)) {
      value = Boolean.parseBoolean(rawValue.substring(2));
    }

    if (rawValue.startsWith(PRIMITIVE_JSO_TYPE_PREFIX + PRIMITIVE_SEPARATOR)) {
      JsonParser parser = new JsonParser();
      value = parser.parse(rawValue.substring(3));
    }

    return new UnmutablePrimitive(value);
  }

  /**
   * Deserialize a node reference.
   *
   * @param rawValue
   * @return
   */
  private UnmutableNode deserializeValue(String rawValue) {

    Preconditions.checkNotNull(rawValue, "Unable to deserialize a null value");

    SubstrateId substrateId = SubstrateId.deserialize(rawValue);
    if (substrateId != null) {

      if (substrateId.isList())
        return materializeList(substrateId);

      if (substrateId.isMap())
        return materializeMap(substrateId);

      if (substrateId.isText())
        return null; // materializeText(substrateId, null);

      return null;

    } else {

      return deserializePrimitive(rawValue);

    }

  }

  protected UnmutableText materializeText(SubstrateId substrateId) {

    Preconditions.checkArgument(substrateId.isText(), "Not a text substrate id");

    Document doc = getDocument(substrateId);

    return new UnmutableText(doc);

  }

  /**
   * Materialize a key-value pair stored in a wave document's element standing
   * for a map.
   *
   *
   * @param doc
   * @param element
   * @return
   */
  protected Entry<String, UnmutableNode> materializeMapEntry(Document doc, Doc.E element) {

    if (!doc.getTagName(element).equals(WaveCommons.MAP_ENTRY_TAG))
      return null;

    String key = doc.getAttribute(element, WaveCommons.MAP_ENTRY_KEY_ATTR);
    String rawValue = doc.getAttribute(element, WaveCommons.MAP_ENTRY_VALUE_ATTR);
    UnmutableNode value = deserializeValue(rawValue);

    return new Entry<String, UnmutableNode>() {

      @Override
      public String getKey() {
        return key;
      }

      @Override
      public UnmutableNode getValue() {
        return value;
      }

      @Override
      public UnmutableNode setValue(UnmutableNode value) {
        return null;
      }

    };
  }

  /**
   * Materialize a map from a substrate id.
   *
   * @param substrateId
   * @return
   * @throws WaveServerException
   */
  protected UnmutableMap materializeMap(SubstrateId substrateId) {

    Preconditions.checkArgument(substrateId.isMap(), "Not a map substrate id");

    Document doc = getDocument(substrateId);
    Doc.E mapElement = DocHelper.getElementWithTagName(doc, WaveCommons.MAP_TAG);
    Doc.E entryElement = DocHelper.getFirstChildElement(doc, mapElement);

    Map<String, UnmutableNode> map = new HashMap<String, UnmutableNode>();

    while (entryElement != null) {
      Entry<String, UnmutableNode> mapEntry = materializeMapEntry(doc, entryElement);
      map.put(mapEntry.getKey(), mapEntry.getValue());
      entryElement = DocHelper.getNextSiblingElement(doc, entryElement);
    }

    return new UnmutableMap(map);
  }

  /**
   * Materialize a value from a list item in a wave document.
   *
   * @param doc
   * @param element
   * @return
   */
  protected UnmutableNode materializeListEntry(Document doc, Doc.E element) {

    if (!doc.getTagName(element).equals(WaveCommons.LIST_ENTRY_TAG))
      return null;

    String rawValue = doc.getAttribute(element, WaveCommons.LIST_ENTRY_VALUE_ATTR);
    UnmutableNode value = deserializeValue(rawValue);

    return value;

  }

  protected UnmutableList materializeList(SubstrateId substrateId) {

    Preconditions.checkArgument(substrateId.isList(), "Not a list substrate id");

    Document doc = getDocument(substrateId);
    Doc.E listElement = DocHelper.getElementWithTagName(doc, WaveCommons.LIST_TAG);
    Doc.E entryElement = DocHelper.getFirstChildElement(doc, listElement);

    List<UnmutableNode> list = new ArrayList<UnmutableNode>();

    while (entryElement != null) {
      UnmutableNode listEntry = materializeListEntry(doc, entryElement);
      list.add(listEntry);
      entryElement = DocHelper.getNextSiblingElement(doc, entryElement);
    }

    return new UnmutableList(list);

  }

}
