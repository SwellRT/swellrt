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


package org.waveprotocol.wave.client.doodad.attachment;

import org.waveprotocol.wave.media.model.AttachmentV3;
import org.waveprotocol.wave.media.model.ClientAttachment;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 *
 */
public interface SimpleAttachmentManager {

  public enum UploadStatusCode {
    NOT_UPLOADING,
    SUCCEEDED,
    IN_PROGRESS,
    FAILED_AND_RETRYABLE,
    FAILED_AND_NOT_RETRYABLE;
  }

  public interface Listener {
    void onContentUpdated(Attachment attachment);
    void onThumbnailUpdated(Attachment attachment);
    void onUploadStatusUpdated(Attachment attachment);
  }

  public interface Attachment extends ClientAttachment, AttachmentV3 {
    UploadStatusCode getUploadStatusCode();
    double getUploadStatusProgress();
  }

  Attachment getAttachment(String id);


  public void addListener(Listener l);

  public void removeListener(Listener l);
}
