package org.swellrt.model.shared;

import org.swellrt.model.ReadableModel;
import org.swellrt.model.ReadableType;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.MetadataContainer;
import org.swellrt.model.generic.TextType;
import org.swellrt.model.generic.ValuesContainer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;

public class ModelUtils {

  public static String serialize(WaveId waveId) {
    return ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId);
  }

  public static String serialize(WaveletId waveletId) {
    return ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId);
  }

  public static boolean isMapBlip(String blipId) {
    return (blipId != null) && (blipId.startsWith(MapType.PREFIX));
  }

  public static boolean isListBlip(String blipId) {
    return (blipId != null) && (blipId.startsWith(ListType.PREFIX));
  }

  public static boolean isTextBlip(String blipId) {
    return (blipId != null) && (blipId.startsWith(TextType.PREFIX));
  }

  public static String getContainerValue(ReadableBlipData blip, String valueRef) {

    int index = -1;
    try {
      index = Integer.valueOf(valueRef.substring(4));
    } catch (NumberFormatException e) {
      return null;
    }

    Document document = blip.getContent().getMutableDocument();
    Doc.E eltValues = DocHelper.getElementWithTagName(document, ValuesContainer.TAG_VALUES);

    if (eltValues == null) return null;

    Doc.E element = DocHelper.getFirstChildElement(document, eltValues);
    String value = document.getAttribute(element, ValuesContainer.ATTR_VALUE);

    int c = 0;
    while (element != null && c < index) {
      element = DocHelper.getNextSiblingElement(document, element);
      value = document.getAttribute(element, ValuesContainer.ATTR_VALUE);
      c++;
    }

    return c == index ? value : null;
  }

  public static boolean isContainerId(String refId) {
    return refId.startsWith(MapType.PREFIX + "+") || refId.startsWith(ListType.PREFIX + "+");
  }

  public static ReadableType fromPath(ReadableModel model, String path) {
    String[] pathKeys = path.split("\\.");

    if (pathKeys == null || pathKeys.length == 0 || !pathKeys[0].equalsIgnoreCase("root")) {
      return null;
    }

    ReadableType currentObject = model.getRoot();
    boolean isLeaf = false;

    for (int i = 1; i < pathKeys.length; i++) {

      // Unconsistencies on the path
      if (currentObject == null) return null;
      if (isLeaf) return null;

      String key = pathKeys[i];

      if (currentObject.asMap() != null) {

        currentObject = currentObject.asMap().get(key);

      } else if (currentObject.asList() != null) {

        int index = -1;
        try {
          index = Integer.parseInt(key);
        } catch (NumberFormatException e) {
          return null;
        }

        if (index < 0 || index >= currentObject.asList().size()) return null;

        currentObject = (ReadableType) currentObject.asList().get(index);

      } else if (currentObject.asText() != null ||
                 currentObject.asFile() != null ||
                 currentObject.asNumber() != null ||
                 currentObject.asBoolean() != null ||
                 currentObject.asString() != null)
      {
        isLeaf = true;
      }

    }

    return currentObject;
  }


  public static String getMetadataPath(Document doc) {
    Doc.E element = DocHelper.getElementWithTagName(doc, MetadataContainer.TAG_METADATA);
    if (element != null)
      return doc.getAttribute(element, MetadataContainer.ATTR_PATH);

    return null;
  }

}
