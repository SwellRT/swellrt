/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.client.common.util;

import com.google.common.base.Joiner;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A value object for an RGB triple.
 *
 * @author hearnden@google.com (David Hearnden)
 */
@JsType(namespace = "swell", name = "Color")
public final class RgbColor {

  @JsIgnore
  public static final RgbColor BLACK = new RgbColor(0, 0, 0);
  @JsIgnore
  public static final RgbColor WHITE = new RgbColor(255, 255, 255);

  public final int red;
  public final int green;
  public final int blue;

  @JsIgnore
  public RgbColor(int red, int green, int blue) {
    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  @JsIgnore
  public RgbColor(String hex) {
    if (hex.startsWith("#"))
      hex = hex.substring(1); // remove leading #
    this.red = Integer.valueOf(hex.substring(0, 2), 16);
    this.green = Integer.valueOf(hex.substring(2, 4), 16);
    this.blue = Integer.valueOf(hex.substring(4, 6), 16);
  }

  /** @return the CSS color expression for this color. */
  @JsProperty
  public String getCssColor() {
    return "rgb(" + red + "," + green + "," + blue + ")";
  }

  /** @return the Hex RGB value for this color */
  @JsProperty
  public String getHexColor() {
    return Joiner.on("").join("#", Integer.toHexString(red), Integer.toHexString(green), Integer.toHexString(blue));
  }

  @Override
  @JsIgnore
  public int hashCode() {
    // Bitshifting is significantly faster on both Chrome and Firefox.
    return (red << 16) | (green << 8) | blue;
  }

  @Override
  @JsIgnore
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof RgbColor)) {
      return false;
    } else {
      RgbColor color = (RgbColor) o;
      return red == color.red && green == color.green && blue == color.blue;
    }
  }
}
