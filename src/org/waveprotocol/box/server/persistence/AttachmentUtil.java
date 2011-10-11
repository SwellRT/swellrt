/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.server.persistence;

import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Some utility methods for managing attachment data objects
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AttachmentUtil {
  private AttachmentUtil() {}
  
  /**
   * Write an input stream to an output stream. This will often be useful for
   * implementors of AttachmentData.writeDataTo().
   * 
   * @param source The InputStream to read from
   * @param dest The OutputStream to write to
   * @throws IOException
   */
  public static void writeTo(InputStream source, OutputStream dest) throws IOException {
    byte[] buffer = new byte[256];
    int length;
    while ((length = source.read(buffer)) != -1) {
      dest.write(buffer, 0, length);
    }
  }

  /**
   * Write the attachment out to a string.
   * 
   * @param encoding The string encoding format of the data. Eg, "UTF-8".
   * @return A string representation of the attachment data.
   * @throws IOException
   */
  public static String writeAttachmentDataToString(
      AttachmentData data, String encoding) throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    data.writeDataTo(stream);
    return stream.toString(encoding);
  }
}
