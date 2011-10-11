/**
 * Copyright 2009 Google Inc.
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
package org.waveprotocol.wave.media.model;


/**
 * Represents a set of client-side attachment metadata for a single attachment
 * instance on a wavelet.
 *
 * Implementations of this class should be thread-safe.
 *
 * TODO(user): This interface will eventually replace the existing
 * org.waveprotocol.wave.model.wave.Attachment interface. Once this
 * is done, remove the old code.
 *
 */
// TODO(user): Replace all uses of this interface by either AttachmentV3, or a server-side
// extension of that interface.
public interface ClientAttachment {
  /**
   * Gets the relative URL by which this attachment may be retrieved.
   *
   * @return a relative URL or null if the attachment is not yet ready to
   * download
   */
  String getAttachmentUrl();

  /**
   * Gets the total size of the attachment in bytes if known, or null
   * otherwise.
   *
   * @return the total size of the attachment, or null.
   */
  Long getSize();

  /**
   * Gets the user-provided filename of the attachment. This may have been
   * sanitized for safer consumption.
   *
   * @return the attachment filename or null if the filename is not known
   */
  String getFilename();

  /**
   * Gets the MIME type of the attachment if known.
   *
   * @return the MIME type as a {@code String} or {@code null} if unknown
   */
  String getMimeType();

  /**
   * Gets the image annotation data for the attachment.
   *
   * @return the ImageAnnotation or null if the attachment is not an image
   * or the metadata is not yet known
   */
  Image getImage();

  /**
   * Gets the thumbnail annotation data for the attachment.
   *
   * @return the ThumbnailAnnotation or null if there is no thumbnail present
   */
  Thumbnail getThumbnail();

  /**
   * Gets the relative URL by which this thumbnail may be retrieved.
   *
   * @return a relative URL
   */
  String getThumbnailUrl();

  /**
   * Gets the upload progress of the attachment as bytes. When this value is
   * non-zero and equal to the value returned by {@link #getSize},
   * the attachment upload is complete.
   *
   * @return the upload progress of the attachment in bytes
   */
  long getUploadedByteCount();

  /**
   * The number of times the upload for this attachment has been retried.
   *
   * @return the number of times the upload has been retried.
   */
  long getUploadRetryCount();

  /**
   * Gets the malware status of the attachment.
   *
   * @return true if the attachment is known or suspected to be malware, false
   * if it is either known not to be malware or if the malware status is unknown
   */
  boolean isMalware();

  /**
   * The upload status of the attachment.
   *
   * @return the status of the upload.
   */
  String getStatus();

  /**
   * Gets the user id of the user who created the attachment.
   *
   * @return the user id or null if the creator is unknown (may occur if an
   * attach request comes in before the creation request)
   */
  String getCreator();

  /**
   * The thumbnail part of the Attachment metadata.
   */
  interface Thumbnail {
    /**
     * The width of the thumbnail in pixels.
     *
     * @return the width in pixels
     */
    int getWidth();

    /**
     * The height of the thumbnail in pixels.
     *
     * @return the height in pixels
     */
    int getHeight();
  }

  /**
   * The image part of the Attachment metadata. Only used if the attachment
   * is an image.
   */
  interface Image {
    /**
     * Gets the height of the image attachment.
     *
     * @return the height of the image in pixels
     */
    int getHeight();


    /**
     * Gets the width of the image attachment.
     *
     * @return the width of the image in pixels
     */
    int getWidth();
  }
}
