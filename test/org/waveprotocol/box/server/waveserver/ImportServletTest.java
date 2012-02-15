/**
 * Copyright 2012 A. Kaplanov
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

package org.waveprotocol.box.server.waveserver;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;

import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class ImportServletTest extends TestCase implements TestingConstants {

  private static final HashedVersion VERSION = HashedVersion.unsigned(111L);

  private static final String FROM_DOMAIN = "googlewave.com";
  private static final String FROM_USER = USER_NAME + "@" + FROM_DOMAIN;

  private static final ProtocolWaveletDelta DELTA_ADD_USER = ProtocolWaveletDelta.newBuilder()
    .setAuthor(FROM_USER)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setAddParticipant(FROM_USER)).build();

  private static final ProtocolWaveletDelta DELTA_REMOVE_USER = ProtocolWaveletDelta.newBuilder()
    .setAuthor(FROM_USER)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setRemoveParticipant(FROM_USER)).build();

  private static final ProtocolWaveletDelta DELTA_FROM_PUBLIC_USER = ProtocolWaveletDelta.newBuilder()
    .setAuthor(ImportServlet.GWAVE_PUBLIC_USER_NAME + "@" + ImportServlet.GWAVE_PUBLIC_DOMAIN)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setNoOp(true)).build();

  @Mock
  LocalWaveletContainerImpl wavelet;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(wavelet.getCurrentVersion()).thenReturn(VERSION);
  }

  public void testSubstitutionOfUserName() throws WaveletStateException, InvalidParticipantAddress {
    ProtocolWaveletDelta delta = ImportServlet.convertDelta(DELTA_ADD_USER, DOMAIN, wavelet,
        WAVELET_NAME, new HashSet<ParticipantId>());
    assertEquals(USER, delta.getAuthor());
    assertEquals(USER, delta.getOperation(0).getAddParticipant());
  }

  public void testSkippingAdditionOfDuplicateUser() throws WaveletStateException, InvalidParticipantAddress {
    Set<ParticipantId> participants = new HashSet<ParticipantId>();
    ProtocolWaveletDelta delta;

    delta = ImportServlet.convertDelta(DELTA_ADD_USER, DOMAIN, wavelet,
        WAVELET_NAME, participants);

    delta = ImportServlet.convertDelta(DELTA_ADD_USER, DOMAIN, wavelet,
        WAVELET_NAME, participants);

    assertTrue(delta.getOperation(0).hasNoOp());
  }

  public void testSkippingRemovalOfNonExistentUser() throws InvalidParticipantAddress {
    ProtocolWaveletDelta delta = ImportServlet.convertDelta(DELTA_REMOVE_USER, DOMAIN, wavelet,
        WAVELET_NAME, new HashSet<ParticipantId>());
    assertTrue(delta.getOperation(0).hasNoOp());
  }

  public void testSubstitutionOfPublicUserName() throws WaveletStateException, InvalidParticipantAddress {
    ProtocolWaveletDelta delta = ImportServlet.convertDelta(DELTA_FROM_PUBLIC_USER, DOMAIN, wavelet,
        WAVELET_NAME, new HashSet<ParticipantId>());
    assertEquals(ImportServlet.WIAB_SHARED_USER_NAME + "@" + DOMAIN, delta.getAuthor());
  }

}
