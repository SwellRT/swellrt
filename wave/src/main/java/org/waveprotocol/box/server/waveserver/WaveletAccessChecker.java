package org.waveprotocol.box.server.waveserver;

import javax.inject.Inject;

import org.apache.commons.lang.RandomStringUtils;
import org.waveprotocol.box.server.CoreSettingsNames;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import com.google.inject.name.Named;

/**
 * Handle access control logic for wavelets.
 *
 * @author pablojan@gmail.com
 *
 */
public class WaveletAccessChecker {

  private static final Log LOG = Log.get(WaveletAccessChecker.class);

  private final ParticipantId sharedPid;
  private final ParticipantId superPid;
  private final String waveDomain;

  @Inject
  public WaveletAccessChecker(
      @Named(CoreSettingsNames.WAVE_SERVER_DOMAIN) final String waveDomain) {
    this.waveDomain = waveDomain;
    String randomId = RandomStringUtils.randomAlphanumeric(10).toLowerCase();
    this.superPid = ParticipantIdUtil.makeSuperPartincipantId(randomId, waveDomain);
    this.sharedPid = ParticipantIdUtil.makeUnsafeSharedDomainParticipantId(waveDomain);
    LOG.info("Random Super Participant Id is " + superPid.toString());
  }

  /**
   * Checks if the user has access to the wavelet.
   *
   * @param snapshot
   *          the wavelet data.
   * @param user
   *          the user that wants to access the wavelet.
   * @param sharedDomainParticipantId
   *          the shared domain participant id.
   * @param superParticipantId
   *          id for the super user.
   * @return true if the user has access to the wavelet.
   */
  public boolean checkAccessPermission(ReadableWaveletData snapshot, ParticipantId user) {
    return user != null && (snapshot == null || snapshot.getParticipants().contains(user)
        || (sharedPid != null && snapshot.getParticipants().contains(sharedPid)));
  }

  public ParticipantId getSharedParticipantId() {
    return sharedPid;
  }

}
