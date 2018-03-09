package org.waveprotocol.box.server.persistence;

import java.util.List;

import org.waveprotocol.wave.model.wave.ParticipantId;

public interface GroupStore {

  public static class GroupData {

    public final ParticipantId groupId;
    public final String name;
    public final ParticipantId[] participants;

    protected GroupData(ParticipantId groupId, String name, ParticipantId[] participants) {
      super();
      this.groupId = groupId;
      this.name = name;
      this.participants = participants;
    }

  }

  public void putGroup(GroupData group);

  public void removeGroup(GroupData group);

  public GroupData getGroup(ParticipantId gropuId);

  public List<GroupData> queryGroupsWithParticipant(ParticipantId participantId);

}
