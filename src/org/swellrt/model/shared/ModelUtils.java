package org.swellrt.model.shared;

import org.swellrt.model.ReadableModel;
import org.swellrt.model.ReadableType;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.TextType;

public class ModelUtils {

  public static ReadableType fromPath(ReadableModel model, String path) {
    String[] pathKeys = path.split(".");

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

      if (currentObject instanceof MapType) {

        currentObject = ((MapType) currentObject).get(key);

      } else if (currentObject instanceof ListType) {

        int index = -1;
        try {
          index = Integer.parseInt(key);
        } catch (NumberFormatException e) {
          return null;
        }

        if (index < 0 || index >= ((ListType) currentObject).size()) return null;

        currentObject = ((ListType) currentObject).get(index);

      } else if (currentObject instanceof StringType) {

        isLeaf = true;

      } else if (currentObject instanceof TextType) {

        isLeaf = true;
      }

    }

    return currentObject;
  }

}
