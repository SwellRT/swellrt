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
 * An attachment. This interface is used both by the client (web-app) and on the server-side.
 *
 */
// TODO(user): Rename this to just Attachment, after deleting:
//   + ClientAttachment.java
public interface AttachmentV3 {
  /** Metadata for an image, whether the original attachment or a thumbnail. */
  public interface ImageMetadata {
    /** Returns the image width. */
    public int getWidth();
    /** Returns the image height. */
    public int getHeight();
  }

  /** Enumeration of possible error conditions. */
  public enum ErrorCondition {
    /** The uploaded file is larger than the per file limit. */
    FILE_TOO_LARGE,

    /** The user has exceeded their storage quota. */
    QUOTA_EXCEEDED,

    /** The upload has been retried the maximum number of times. */
    TOO_MANY_RETRIES;
  }

  /** Returns the attachmentId, which should never be null. */
  public String getAttachmentId();

  /** Returns the creator's userId (e.g. foo@example.com), or null if unavailable. */
  public String getCreator();

  /** Returns the attachment's MIME type, or null if unavailable. */
  public String getMimeType();

  /** Returns whether the attachment contains malware. */
  public boolean isMalware();

  /**
   * Returns the original attachment's metadata, if the attachment is an image, or null if
   * unavailable.
   */
  public ImageMetadata getContentImageMetadata();

  /** Returns the thumbnail image's metadata, or null if unavailable. */
  public ImageMetadata getThumbnailImageMetadata();
}
