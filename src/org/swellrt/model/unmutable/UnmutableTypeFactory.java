package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableType;
import org.swellrt.model.adt.UnmutableElementList;
import org.swellrt.model.generic.FileType;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.id.InvalidIdException;

public class UnmutableTypeFactory {


  public static ReadableType deserialize(UnmutableModel model,
      UnmutableElementList<String, Void> valuesContainer,
      String ref) {


    if (ref.startsWith(StringType.PREFIX)) {
      int index = Integer.valueOf(ref.substring(StringType.PREFIX.length()+1));
      return new UnmutableString(valuesContainer.get(index));

    } else if (ref.startsWith(MapType.PREFIX)) {
      return UnmutableMap.deserialize(model, ref);

    } else if (ref.startsWith(ListType.PREFIX)) {
      return UnmutableList.deserialize(model, ref);

    } else if (ref.startsWith(TextType.PREFIX)) {
      return new UnmutableText(model.getBlipData(ref));

    } else if (ref.startsWith(FileType.PREFIX)) {

      int index = Integer.valueOf(ref.substring(FileType.PREFIX.length() + 1));
      try {
        return new UnmutableFile(AttachmentId.deserialise(valuesContainer.get(index)));
      } catch (InvalidIdException e) {
        // TODO handle exception
        return null;
      }
    }

    return null;
  }



}
