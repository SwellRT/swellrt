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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * An attachment store is a place for storing attachment data. This does not
 * store any attachment metadata (thats kept in the document store).
 * 
 * Each attachment has an id, which associates the attachment with a binary blob.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public interface AttachmentStore {
  /**
   * An attachment data object exposes the data for a single attachment blob.
   */
  public interface AttachmentData {
    /**
     * Get the size of the attachment's data
     * @return The size of the attachment data segment, in bytes.
     */
    public abstract long getContentSize();
    
    /**
     * Get the last modified date of the attachment. This will be the date the
     * attachment data was written.
     * @return The date the attachment data was last modified
     */
    public abstract Date getLastModifiedDate();
    
    /**
     * Write the attachment's data to the specified output stream.
     * 
     * @param out The stream to write the attachment out to.
     */
    public void writeDataTo(OutputStream out) throws IOException;
    
    /**
     * Get the attachment data object as a stream.
     * 
     * This method must return a new stream each time it is called.
     * 
     * Callers should be aware that requesting multiple input streams may result
     * in extra fetches from the database. 
     * 
     * Note: Callers MUST CLOSE THE INPUT STREAM when they're done with it.
     * 
     * @return The data as an input stream.
     * @throws IOException
     */
    public abstract InputStream getInputStream() throws IOException;
  }
  
  /**
   * Fetch an attachment with the specified id.
   * 
   * @param id
   * @return the attachment with the specified id, or null if the attachment
   * does not exist
   */
  AttachmentData getAttachment(String id);
  
  /**
   * Store a new attachment with the specified id and data.
   * 
   * If there is already an attachment with the provided id, storeAttachment
   * does nothing and returns false. If there is no attachment with the provided
   * id, the function stores a new attachment and returns true.
   *  
   * @param id The id of the attachment
   * @param data A stream which contains the data to be stored
   * @throws IOException
   * @returns true if the data was successfully stored. False otherwise.
   */
  boolean storeAttachment(String id, InputStream data) throws IOException;

  /**
   * Delete the specified attachment from the store. If the attachment does
   * not exist, this has no effect.
   * 
   * The behavior of calling any methods on an open AttachmentData object is
   * undefined (implementation-specific).
   * 
   * @param id
   */
  void deleteAttachment(String id);
}
