package org.swellrt.server.box.events;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Map;


public class Event {

  public enum Type {

    DOC_CHANGE("DOC_CHANGE"),
    MAP_ENTRY_UPDATED("MAP_ENTRY_UPDATED"), // includes creation
    MAP_ENTRY_REMOVED("MAP_ENTRY_REMOVED"),
    LIST_ITEM_ADDED("LIST_ITEM_ADDED"),
    LIST_ITEM_REMOVED("LIST_ITEM_REMOVED"),
    ADD_PARTICIPANT("ADD_PARTICIPANT"),
    REMOVE_PARTICIPANT("REMOVE_PARTICIPANT");

    String str;

    Type(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
      return str;
    }
  }


  public static class Builder {

    private Map<String, String> contextData;

    private String app;
    private String dataType;
    private WaveId waveId;
    private WaveletId waveletId;
    private String blipId;
    private HashedVersion deltaVersion;
    private String author;
    private long timestamp;


    public Builder app(String app) {
      this.app = app;
      return this;
    }

    public Builder dataType(String dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder waveId(WaveId waveId) {
      this.waveId = waveId;
      return this;
    }

    public Builder waveletId(WaveletId waveletId) {
      this.waveletId = waveletId;
      return this;
    }

    public Builder blipId(String blipId) {
      this.blipId = blipId;
      return this;
    }


    public Builder deltaVersion(HashedVersion deltaVersion) {
      this.deltaVersion = deltaVersion;
      return this;
    }

    public Builder contextData(Map<String, String> contextData) {
      this.contextData = contextData;
      return this;
    }

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }


    public Map<String, String> getContextData() {
      return this.contextData;
    }

    public String getBlipId() {
      return this.blipId;
    }

    public Event build(Type type) {
      return new Event(timestamp, author, waveId, waveletId, app, dataType, deltaVersion, type,
          contextData);
    }

    /**
     * Create a new event. Values of arguments overwrite existing values of the
     * builder.
     *
     * @param type
     * @param path
     * @param specificContextData
     * @return
     */
    public Event build(Type type, String path, Map<String, String> specificContextData) {
      return new Event(timestamp, author, waveId, waveletId, app, dataType, deltaVersion, blipId,
          path, type,
          specificContextData);
    }

    public Event build(Type type, String path) {
      return new Event(timestamp, author, waveId, waveletId, app, dataType, deltaVersion, blipId,
          path, type,
          contextData);
    }

    public Event buildAddParticipant(ParticipantId participantId) {
      return new Event(timestamp, author, waveId, waveletId, app, dataType, deltaVersion,
          Event.Type.ADD_PARTICIPANT,
          contextData, participantId);
    }

    public Event buildRemoveParticipant(ParticipantId participantId) {
      return new Event(timestamp, author, waveId, waveletId, app, dataType, deltaVersion,
          Event.Type.REMOVE_PARTICIPANT, contextData, participantId);
    }

    public Event buildDocChange(String path, String characters) {
      return new Event(timestamp, author, waveId, waveletId, app, dataType, deltaVersion, blipId,
          path,
          Event.Type.DOC_CHANGE, contextData, characters);
    }

  }

  private final Map<String, String> contextData;

  private final String app;
  private final String dataType;
  private final WaveId waveId;
  private final WaveletId waveletId;
  private String blipId;
  private final HashedVersion deltaVersion;
  private final String author;
  private final long timestamp;

  private final Event.Type type;
  private String path;

  /** Added or removed characters in text ops */
  private String characters;
  /** Added or removed participant */
  private String participant;

  protected Event(long timestamp, String author, WaveId waveId, WaveletId waveletId, String app,
      String dataType,
      HashedVersion deltaVersion, String blipId,
 String path, Type type,
      Map<String, String> contextData) {
    super();
    this.timestamp = timestamp;
    this.author = author;
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.app = app;
    this.dataType = dataType;
    this.blipId = blipId;
    this.deltaVersion = deltaVersion;
    this.path = path;
    this.type = type;
    this.contextData = contextData;
  }

  protected Event(long timestamp, String author, WaveId waveId, WaveletId waveletId, String app,
      String dataType,
      HashedVersion deltaVersion, String blipId, String path, Type type,
      Map<String, String> contextData, String characters) {
    super();
    this.timestamp = timestamp;
    this.author = author;
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.app = app;
    this.dataType = dataType;
    this.blipId = blipId;
    this.deltaVersion = deltaVersion;
    this.path = path;
    this.type = type;
    this.contextData = contextData;
    this.characters = characters;
  }

  protected Event(long timestamp, String author, WaveId waveId, WaveletId waveletId, String app,
      String dataType,
      HashedVersion deltaVersion, Type type, Map<String, String> contextData) {
    super();
    this.timestamp = timestamp;
    this.author = author;
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.app = app;
    this.dataType = dataType;
    this.deltaVersion = deltaVersion;
    this.type = type;
    this.contextData = contextData;
  }


  protected Event(long timestamp, String author, WaveId waveId, WaveletId waveletId, String app,
      String dataType,
      HashedVersion deltaVersion, Type type, Map<String, String> contextData,
      ParticipantId participantId) {
    super();
    this.timestamp = timestamp;
    this.author = author;
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.app = app;
    this.dataType = dataType;
    this.deltaVersion = deltaVersion;
    this.type = type;
    this.contextData = contextData;
    this.participant = participantId.getAddress();
  }

  public Map<String, String> getContextData() {
    return contextData;
  }

  public String getApp() {
    return app;
  }

  public String getDataType() {
    return dataType;
  }

  public WaveId getWaveId() {
    return waveId;
  }

  public WaveletId getWaveletId() {
    return waveletId;
  }

  public Event.Type getType() {
    return type;
  }

  public String getPath() {
    return path;
  }

  public String getBlipId() {
    return blipId;
  }

  public HashedVersion getDeltaVersion() {
    return deltaVersion;
  }

  public String getAuthor() {
    return this.author;
  }

  public String getParticipant() {
    return this.participant;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public String getCharacters() {
    return characters;
  }


}
