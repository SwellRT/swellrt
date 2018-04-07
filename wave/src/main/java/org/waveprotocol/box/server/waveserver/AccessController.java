package org.waveprotocol.box.server.waveserver;

import javax.inject.Inject;

import org.apache.commons.lang.RandomStringUtils;
import org.waveprotocol.box.server.CoreSettingsNames;
import org.waveprotocol.box.server.persistence.GroupStore;
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
public class AccessController {

  private static final Log LOG = Log.get(AccessController.class);

  private final ParticipantId anyonePid;
  private final ParticipantId anyoneRegularPid;
  private final ParticipantId superPid;

  private final String waveDomain;
  private final GroupStore groupStore;

  @Inject
  public AccessController(
      @Named(CoreSettingsNames.WAVE_SERVER_DOMAIN) final String waveDomain, GroupStore groupStore) {
    this.waveDomain = waveDomain;

    String randomId = RandomStringUtils.randomAlphanumeric(10).toLowerCase();

    this.superPid = ParticipantIdUtil.makeSuperPartincipantId(randomId, waveDomain);
    this.anyonePid = ParticipantIdUtil.makeAnyoneParticipantId(waveDomain);
    this.anyoneRegularPid = ParticipantIdUtil.makeAnyoneRegularId(waveDomain);

    this.groupStore = groupStore;
    LOG.info("Random Super Participant Id is " + superPid.toString());
  }


  /**
   * Checks if the user has access to the wavelet.
   *
   * @param snapshot
   *          the wavelet data.
   * @param user
   *          the user that wants to access the wavelet.
   *
   * @return true if the user has access to the wavelet.
   */
  public boolean checkAccessPermission(ReadableWaveletData snapshot, ParticipantId user) {

    if (user == null)
      return false;

    if (snapshot == null)
      return true;

    if (user.equals(superPid))
      return true;

    // check single participant
    if (snapshot.getParticipants().contains(user))
      return true;

    if (snapshot.getParticipants().contains(anyonePid))
      return true;

    if (user.isRegular() && snapshot.getParticipants().contains(anyoneRegularPid))
      return true;


    // check groups
    // snapshot.getParticipants().stream().filter(ParticipantId::isGroup).filter(groupId
    // => { groupStore.getGroup(groupId); return true; });


    return false;
  }

  public ParticipantId getSharedParticipantId() {
    return anyoneRegularPid;
  }

}
