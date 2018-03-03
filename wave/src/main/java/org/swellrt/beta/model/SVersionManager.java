package org.swellrt.beta.model;

import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.model.json.SJsonObject;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Object and its node's versions can be tagged. This is specially interesting
 * for SText nodes.
 *
 * @author pablojan@gmail.com
 *
 */
@JsType(namespace = "swell", name = "VersionManager")
public interface SVersionManager {

  @JsType(namespace = "swell.VersionManager", name = "Tag")
  public class Tag {

    @JsIgnore
    public static Tag fromSJson(SJsonObject sjson) {
      Tag tag = new Tag();
      tag.name = sjson.getString("name");
      tag.description = sjson.getString("description");
      tag.author = ParticipantId.ofUnsafe(sjson.getString("author"));
      tag.timestamp = sjson.getDouble("timestamp");
      try {
        tag.version = HashedVersion.valueOf(sjson.getString("version"));
      } catch (NumberFormatException | Base64DecoderException e) {
        throw new IllegalStateException(e);
      }
      return tag;
    }

    @JsIgnore
    public SJsonObject toSJson() {
      SJsonObject sjson = WaveDeps.sJsonFactory.create();
      sjson.addString("version", version.serialise());
      sjson.addDouble("timestamp", timestamp);
      sjson.addString("name", name);
      sjson.addString("description", description);
      sjson.addString("author", author.getAddress());
      return sjson;
    }

    @JsIgnore
    public HashedVersion version;
    public double timestamp;
    public String name;
    public String description;
    public ParticipantId author;

    @JsProperty
    public double getVersion() {
      return version.getVersion();
    }
  }

  public Tag tag(SNode node, String tagName, String tagDescription);

  public Tag[] getTags(SNode node);

}
