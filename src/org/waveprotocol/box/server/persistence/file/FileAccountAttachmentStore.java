package org.waveprotocol.box.server.persistence.file;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.codec.binary.Base64;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.AccountAttachmentStore;
import org.waveprotocol.box.server.persistence.AttachmentUtil;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class FileAccountAttachmentStore implements AccountAttachmentStore {


  private static final Log LOG = Log.get(FileAccountAttachmentStore.class);

  private final static String AVATAR_DIR = "avatar";

  /**
   * The directory in which the attachments are stored.
   */
  private final String basePath;

  /**
   * The local wave domain transformed as a secure path
   */
  private final String domainAsPath;

  @Inject
  public FileAccountAttachmentStore(@Named(CoreSettings.WAVE_SERVER_DOMAIN) String domain,
      @Named(CoreSettings.ACCOUNT_ATTACHMENT_STORE_DIRECTORY) String basePath) {
    this.basePath = basePath;
    this.domainAsPath = getDomainAsPath(domain);
    new File(basePath + File.separatorChar + AVATAR_DIR).mkdirs();

  }

  protected String getDomainAsPath(String domain) {
    return domain.replaceAll("\\.", "_");
  }

  public String storeAvatar(ParticipantId participantId, String mimeType, String base64data,
      String currentAvatarFileId)
      throws IOException {

    String avatarFileId = getAvatarFileId(participantId, mimeType);
    String avatarFilePath = getLocalAvatarPath(avatarFileId);

    File file = new File(avatarFilePath);

    ByteArrayInputStream data = new ByteArrayInputStream(Base64.decodeBase64(base64data));

    FileOutputStream stream = new FileOutputStream(file, false);
    AttachmentUtil.writeTo(data, stream);
    stream.close();

    if (currentAvatarFileId != null) {
      String oldAvatarFilePath = getLocalAvatarPath(currentAvatarFileId);
      File oldFile = new File(oldAvatarFilePath);
      if (!oldFile.delete()) {
        LOG.warning("An old avatar file couldn't be deleted " + oldAvatarFilePath);
      }
    }

    return mimeType + ";" + avatarFileId;
  }


  @Override
  public Attachment getAvatar(String fileName) {

    final File file = new File(getLocalAvatarPath(fileName));
    if (!file.exists()) {
      return null;
    }

    return new Attachment() {

      @Override
      public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
      }

      @Override
      public long getSize() {
        return file.length();
      }

    };
  }


  protected static String getFileExtensionFromMime(String mimeType) {

    if (mimeType.equalsIgnoreCase("image/jpeg"))
      return ".jpg";
    else if (mimeType.equalsIgnoreCase("image/png"))
      return ".png";
    else
      return "";

  }

  protected String getLocalAvatarPath(String avatarFileId) {
    return basePath + File.separatorChar + AVATAR_DIR + File.separatorChar + avatarFileId;
  }


  private static String getAvatarFileId(ParticipantId id, String mimeType) {
    return CharBase64.encodeWebSafe(id.getAddress().getBytes(Charset.forName("UTF-8")), false)
        + "_"
        + System.currentTimeMillis() + getFileExtensionFromMime(mimeType);
  }


}
