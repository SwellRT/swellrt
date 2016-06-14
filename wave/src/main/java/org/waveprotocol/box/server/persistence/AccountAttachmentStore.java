package org.waveprotocol.box.server.persistence;

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.io.InputStream;

public interface AccountAttachmentStore {

  public interface Attachment {

    InputStream getInputStream() throws IOException;

    long getSize();

  }

  String storeAvatar(ParticipantId participantId, String mimeType, String base64data,
      String currentAvatarFileId) throws IOException;


  Attachment getAvatar(String fileName);

}
