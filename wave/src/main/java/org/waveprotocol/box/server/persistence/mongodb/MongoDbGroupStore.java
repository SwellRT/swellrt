package org.waveprotocol.box.server.persistence.mongodb;

import java.util.List;

import org.bson.Document;
import org.waveprotocol.box.server.persistence.GroupStore;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDbGroupStore implements GroupStore {

  private static final String GROUP_COLLECTION = "groups";

  private final MongoCollection<Document> collection;

  protected static MongoDbGroupStore create(MongoDatabase database) {
    Preconditions.checkArgument(database != null,
        "Unable to get reference to mongoDB groups collection");
    MongoCollection<Document> collection = database.getCollection(GROUP_COLLECTION);
    Preconditions.checkArgument(collection != null,
        "Unable to get reference to mongoDB groups collection");

    return new MongoDbGroupStore(collection);
  }

  protected MongoDbGroupStore(MongoCollection<Document> collection) {
    this.collection = collection;
  }

  @Override
  public void putGroup(GroupData group) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeGroup(GroupData group) {
    // TODO Auto-generated method stub

  }

  @Override
  public GroupData getGroup(ParticipantId gropuId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<GroupData> queryGroupsWithParticipant(ParticipantId participantId) {
    // TODO Auto-generated method stub
    return null;
  }

}
