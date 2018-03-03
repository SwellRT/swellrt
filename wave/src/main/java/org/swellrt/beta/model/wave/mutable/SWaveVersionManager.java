package org.swellrt.beta.model.wave.mutable;

import java.util.stream.StreamSupport;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SVersionManager;
import org.swellrt.beta.model.local.SMapLocal;
import org.swellrt.beta.model.presence.SSession;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;

public class SWaveVersionManager implements SVersionManager {

  private static final String VERSIONS_NODE = "versions";

  private final SMap versionsMap;
  private final SSession session;

  public static SWaveVersionManager create(SMap metadataMap, SSession session) {

    SMap versionsMap = null;

    try {

      if (!metadataMap.has(VERSIONS_NODE)) {
        metadataMap.put(VERSIONS_NODE, new SMapLocal());
      }

      versionsMap = metadataMap.pick(VERSIONS_NODE).asMap();

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    return new SWaveVersionManager(versionsMap, session);
  }

  protected SWaveVersionManager(SMap versionsMap, SSession session) {
    this.session = session;
    this.versionsMap = versionsMap;
  }

  private SMap getWaveletTagMap(WaveletId waveletId) {

    try {
      String sWaveletId = waveletId.getId();
      if (!versionsMap.has(sWaveletId)) {
        versionsMap.put(sWaveletId, SMap.create());
      }

      return versionsMap.pick(sWaveletId).asMap();

    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }


  private SList<? extends SNode> getNodeTagList(SWaveNode node) {
    SMap waveletTagMap = getWaveletTagMap(node.getSubstrateId().getContainerId());
    String docId = node.getSubstrateId().getDocumentId();
    try {
      if (!waveletTagMap.has(docId)) {
        waveletTagMap.put(docId, SList.create());
      }

      return waveletTagMap.pick(docId).asList();
    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  @Override
  public Tag tag(SNode node, String tagName, String tagDescription) {
    Preconditions.checkNotNull(node, "Null node");
    Preconditions.checkArgument(tagName != null && tagName.length() > 0 && tagName.length() < 32,
        "Tag name must have max. 32 chars of length");
    Preconditions.checkArgument(tagDescription != null && tagName.length() > 0,
        "Tag description can't be empty or null");

    SWaveNode wnode = (SWaveNode) node;

    Tag tag = new Tag();
    tag.version = wnode.getLastVersion();
    tag.name = tagName;
    tag.description = tagDescription;
    tag.author = session.getParticipantId();
    tag.timestamp = System.currentTimeMillis();

    SList<? extends SNode> tagList = getNodeTagList(wnode);
    try {
      tagList.add(new SPrimitive(tag.toSJson(), new SNodeAccessControl()));
    } catch (SException e) {
      throw new IllegalStateException(e);

    }

    return tag;
  }

  @Override
  public Tag[] getTags(SNode node) {
    Preconditions.checkNotNull(node, "Null node");
    SWaveNode wnode = (SWaveNode) node;
    SList<? extends SNode> tagList = getNodeTagList(wnode);

    return StreamSupport.stream(tagList.values().spliterator(), false).map((SNode n) -> {
      SPrimitive pnode = (SPrimitive) n;
      return Tag.fromSJson(pnode.asSJson());
    }).toArray(Tag[]::new);

  }

}
