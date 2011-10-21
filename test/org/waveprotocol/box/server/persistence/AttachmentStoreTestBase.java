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

import junit.framework.TestCase;

import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Test cases for the Attachment Stores.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public abstract class AttachmentStoreTestBase extends TestCase {

  private final static WaveletName WAVELET_NAME = WaveletName.of(WaveId.of("example.com", "w+abc"),
      WaveletId.of("example.com", "conv+root"));

  public void testStoreReturnsNullForNonexistantId() {
    AttachmentStore store = newAttachmentStore();
    assertNull(store.getAttachment(WAVELET_NAME, "some_madeup_id"));
  }

  public void testStoreCanStoreData() throws Exception {
    String testData = "some file data";
    String id = "id_1";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);

    AttachmentData data = store.getAttachment(WAVELET_NAME, id);
    assertEquals(testData, dataToString(data));
  }

  public void testStoreReturnsNullForWrongWaveletName() throws Exception {
    String testData = "some file data";
    String id = "id_1";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);

    WaveletName otherWaveletName = WaveletName.of(WaveId.of("example.com", "w+abd"),
        WaveletId.of("example.com", "conv+root"));
    AttachmentData data = store.getAttachment(otherWaveletName, id);
    assertNull(data);
  }

  public void testContentLengthMatchesDataSize() throws Exception {
    String testData = "blah blah blah";
    String id = "id_2";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);

    AttachmentData data = store.getAttachment(WAVELET_NAME, id);
    assertEquals(testData.length(), data.getContentSize());
  }

  public void testStoreCanDeleteData() throws Exception {
    String testData = "some day, I'm going to run out of test strings";
    String id = "id_3";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);

    store.deleteAttachment(WAVELET_NAME, id);
    AttachmentData data = store.getAttachment(WAVELET_NAME, id);
    assertNull(data);
  }

  public void testAttachmentCanWriteToOutputStream() throws Exception {
    String testData = "maybe there's some easy way to generate test strings";
    String id = "id_4";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);
    AttachmentData data = store.getAttachment(WAVELET_NAME, id);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    data.writeDataTo(stream);
    assertEquals(testData, stream.toString("UTF-8"));
  }

  public void testAttachmentHasWorkingInputStream() throws Exception {
    String testData = "I suppose these strings don't actually need to be different";
    String id = "id_5";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);
    AttachmentData data = store.getAttachment(WAVELET_NAME, id);

    BufferedReader reader = new BufferedReader(new InputStreamReader(data.getInputStream()));

    StringBuilder builder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      // This little snippet will discard any "\n" characters, but it shouldn't
      // matter.
      builder.append(line);
    }

    assertEquals(testData, builder.toString());
    reader.close();
  }

  public void testGetStreamReturnsNewStream() throws Exception {
    String testData = "There's something quite peaceful about writing tests.";
    String id = "id_6";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);
    AttachmentData data = store.getAttachment(WAVELET_NAME, id);

    InputStream is1 = data.getInputStream();
    InputStream is2 = data.getInputStream();
    assertNotSame(is1, is2);

    int firstByte = is1.read();
    assertSame(firstByte, is2.read());

    // Check that a new input stream created now still has the same first byte.
    InputStream is3 = data.getInputStream();
    assertSame(firstByte, is3.read());

    is1.close();
    is2.close();
    is3.close();
  }

  public void testOverwriteAttachmentReturnsFalse() throws Exception {
    String testData = "First.";
    String id = "id_7";
    AttachmentStore store = makeStoreWithData(WAVELET_NAME, id, testData);

    // A second element added with the same ID should not write.
    boolean written = writeStringDataToAttachmentStore(store, WAVELET_NAME, id, "Second");
    assertFalse(written);

    // Check that the database still contains the original entry
    assertEquals(testData, dataToString(store.getAttachment(WAVELET_NAME, id)));
  }

  // Helpers.
  /**
   * Create and return a new attachment store instance of the type being tested.
   * @return a new attachment store
   */
  protected abstract AttachmentStore newAttachmentStore();

  protected boolean writeStringDataToAttachmentStore(
      AttachmentStore store, WaveletName waveletName, String id, String data) throws IOException {
    return store.storeAttachment(waveletName, id, new ByteArrayInputStream(data.getBytes("UTF-8")));
  }

  protected AttachmentStore makeStoreWithData(WaveletName waveletName, String id, String data)
      throws Exception {
    AttachmentStore store = newAttachmentStore();
    boolean written = writeStringDataToAttachmentStore(store, waveletName, id, data);
    if (!written) {
      throw new RuntimeException("Could not write attachment to store");
    }
    return store;
  }

  protected String dataToString(AttachmentData data) throws IOException {
    return AttachmentUtil.writeAttachmentDataToString(data, "UTF-8");
  }
}
