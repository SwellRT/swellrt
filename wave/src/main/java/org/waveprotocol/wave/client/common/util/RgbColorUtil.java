package org.waveprotocol.wave.client.common.util;

import java.util.Collection;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Some utility methods for colors
 *
 * @author pablojan@apache.org (Pablo Ojanguren)
 *
 */
public class RgbColorUtil {

  /**
   * Generates a lighter color.
   * >
   * https://stackoverflow.com/questions/6615002/given-an-rgb-value-how-do-i-create-a-tint-or-shade
   *
   * @param color
   * @param factor
   * @return
   */
  public static RgbColor getDarker(RgbColor color, double factor) {

    Preconditions.checkArgument(0 <= factor && factor <= 1, "Factor out of range [0,1]");

    Double red = (color.red * ( 1 - factor ));
    Double green = (color.green * ( 1 - factor ));
    Double blue = (color.blue * ( 1 - factor ));

    return new RgbColor(red.intValue(), green.intValue(), blue.intValue());
  }

  public static RgbColor getLighter(RgbColor color, double factor) {

    Preconditions.checkArgument(0 <= factor && factor <= 1, "Factor out of range [0,1]");

    Double red = color.red + ((255 - color.red ) * factor );
    Double green = color.green + ((255 - color.green ) * factor );
    Double blue = color.blue + ((255 - color.blue ) * factor );

    return new RgbColor(red.intValue(), green.intValue(), blue.intValue());
  }

  public static RgbColor average(Collection<RgbColor> colors) {

    int size = colors.size();
    int red = 0, green = 0, blue = 0;
    for (RgbColor color : colors) {
      red += color.red;
      green += color.green;
      blue += color.blue;
    }

    return size == 0 ? RgbColor.BLACK : new RgbColor(red / size, green / size, blue / size);
  }

}
