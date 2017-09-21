package org.waveprotocol.box.server.persistence.mongodb;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.function.Consumer;

import org.bson.Document;
import org.waveprotocol.box.server.persistence.NamingStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.Preconditions;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Class to handle Waves naming. Collection is "naming"
 * <p>
 * Document format is <br>
 * <code>
 *  {
 *    waveid: "",
 *    name: "",
 *    created: (timestamp)
 *  }
 * </code>
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class MongoDbNamingStore implements NamingStore {

  private static final String NAMING_COLLECTION = "naming";

  private static final String WAVEID_FIELD = "waveid";
  private static final String NAME_FIELD = "name";
  private static final String CREATED_FIELD = "created";

  /**
   * The mongo collection exposing Document based api, instead of former
   * BasicDBObject one.
   */
  private final MongoCollection<Document> collection;

  protected static MongoDbNamingStore create(MongoDatabase database) {
    Preconditions.checkArgument(database != null,
        "Unable to get reference to mongoDB naming collection");
    MongoCollection<Document> collection = database.getCollection(NAMING_COLLECTION);
    Preconditions.checkArgument(collection != null,
          "Unable to get reference to mongoDB naming collection");

    return new MongoDbNamingStore(collection);
  }

  protected MongoDbNamingStore(MongoCollection<Document> collection) {
    this.collection = collection;
  }

  @Override
  public WaveNaming getWaveNamingById(WaveId waveId) {
    String strWaveId = ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId);
    final WaveNaming resultMap = new WaveNaming(waveId);

    collection.find(eq(WAVEID_FIELD, strWaveId)).sort(new Document(CREATED_FIELD, -1))
        .forEach(new Consumer<Document>() {

          @Override
          public void accept(Document d) {
            WaveName name = new WaveName(d.getString(NAME_FIELD), d.getLong(CREATED_FIELD));
            resultMap.names.add(name);
          }

        });

    return resultMap;
  }


  @Override
  public WaveNaming getWaveNamingsByName(String name) throws PersistenceException {

    Document d = collection.find(eq(NAME_FIELD, name)).first();
    if (d == null)
      return null;

    try {
      WaveId waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(d.getString(WAVEID_FIELD));
      return getWaveNamingById(waveId);

    } catch (InvalidIdException e) {
      throw new PersistenceException(e);
    }
  }

  protected boolean existWaveName(WaveId waveId, String name) {
    String strWaveId = ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId);
    return collection.count(and(eq(NAME_FIELD, name), eq(WAVEID_FIELD, strWaveId))) > 0;
  }

  protected boolean existName(String name) {
    return collection.count(eq(NAME_FIELD, name)) > 0;
  }

  @Override
  public WaveNaming addWaveName(WaveId waveId, String name) throws PersistenceException {

    if (existName(name))
      throw new PersistenceException("The name already exists");

    if (existWaveName(waveId, name))
      throw new PersistenceException("Wave-to-name mapping already exists");

    collection.insertOne(new Document()
        .append(WAVEID_FIELD, ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId))
        .append(NAME_FIELD, name)
        .append(CREATED_FIELD, System.currentTimeMillis()));


    return getWaveNamingById(waveId);
  }

  @Override
  public WaveNaming removeWaveName(WaveId waveId, String name) {
    String strWaveId = ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId);
    collection.deleteOne(and(eq(NAME_FIELD, name), eq(WAVEID_FIELD, strWaveId)));
    return getWaveNamingById(waveId);
  }

}
