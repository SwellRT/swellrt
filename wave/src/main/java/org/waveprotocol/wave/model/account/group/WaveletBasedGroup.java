package org.waveprotocol.wave.model.account.group;

import java.util.Collections;

import org.waveprotocol.wave.model.account.DocumentBasedRoles;
import org.waveprotocol.wave.model.account.ObservableRoles;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "WaveletBasedGroup")
public class WaveletBasedGroup implements Group {

  private static final String DOC_METADATA = "metadata";
  private static final String DOC_ROLES = "roles";
  private static final String TAG_INFO = "info";

  @JsIgnore
  public static WaveletBasedGroup create(ObservableWavelet wavelet) {

    //
    // A metadata doc to store info and roles
    //

    ObservableDocument docInfo = wavelet.getDocument(DOC_METADATA);
    DocumentEventRouter<N, E, ? extends N> docRouterInfo = DefaultDocumentEventRouter.create(docInfo);

    //
    // info tag
    //

    Element elementInfo = (Element) DocHelper.getElementWithTagName(docInfo, TAG_INFO);
    if (elementInfo == null) {
      elementInfo = (Element) docInfo.createChildElement(docInfo.getDocumentElement(), TAG_INFO, Collections.emptyMap());
    }

    DocBasedGroupProperties properties = DocBasedGroupProperties.create(docRouterInfo,
        elementInfo);

    //
    // Roles doc
    //

    ObservableDocument rolesDocument = wavelet.getDocument(DOC_ROLES);
    DocumentBasedRoles roles = DocumentBasedRoles.create(rolesDocument);

    return new WaveletBasedGroup(wavelet, properties, roles);
  }

  private final ObservableWavelet wavelet;
  private final DocBasedGroupProperties groupProperties;
  private final DocumentBasedRoles groupRoles;

  private WaveletBasedGroup(ObservableWavelet wavelet, DocBasedGroupProperties groupProperties,
      DocumentBasedRoles docBasedRoles) {
    this.wavelet = wavelet;
    this.groupProperties = groupProperties;
    this.groupRoles = docBasedRoles;
  }

  @Override
  public String getName() {
    return groupProperties.getName();
  }

  @Override
  public void setName(String name) {
    groupProperties.setName(name);
  }

  @Override
  public void addListener(Listener listener) {

  }

  @Override
  public void removeListener(Listener listener) {

  }

  @Override
  public ParticipantId getId() {
    try {
      return ParticipantId.of(wavelet.getId().getId(), wavelet.getId().getDomain());
    } catch (InvalidParticipantAddress e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public ParticipantId[] getParticipants() {
    return wavelet.getParticipantIds()
        .toArray(new ParticipantId[wavelet.getParticipantIds().size()]);
  }

  @Override
  public void addParticipant(ParticipantId participant) {
    wavelet.addParticipant(participant);
  }

  @Override
  public void removeParticipant(ParticipantId participant) {
    wavelet.removeParticipant(participant);
  }

  @Override
  public ObservableRoles getRoles() {
    return groupRoles;
  }

}
