/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.server.rpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An attachment servlet is a simple servlet that serves up attachments from a
 * provided store.
 */
@SuppressWarnings("serial")
@Singleton
public class AttachmentServlet extends HttpServlet {
  private static final Log LOG = Log.get(AttachmentServlet.class);

  private final AttachmentStore store;
  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;

  @Inject
  private AttachmentServlet(AttachmentStore store, WaveletProvider waveletProvider,
      SessionManager sessionManager) {
    this.store = store;
    this.waveletProvider = waveletProvider;
    this.sessionManager = sessionManager;
  }

  /**
   * Get the attachment id from the URL in the request.
   *
   * @param request
   * @return the id of the referenced attachment.
   */
  private static String getAttachmentIdFromRequest(HttpServletRequest request) {
    if (request.getPathInfo().length() == 0) {
      return "";
    }

    // Discard the leading '/' in the pathinfo. Whats left will be the
    // attachment id.
    return request.getPathInfo().substring(1);
  }

  /**
   * Get the attachment id from the URL in the request.
   *
   * @param request
   * @return the id of the referenced attachment.
   */
  private static String getFileNameFromRequest(HttpServletRequest request) {
    String fileName = request.getParameter("fileName");
    return fileName != null ? fileName : "";
  }

  private static WaveletName waveRef2WaveletName(String waveRefStr) {
    WaveRef waveRef = null;
    try {
      waveRef = JavaWaverefEncoder.decodeWaveRefFromPath(waveRefStr);
    } catch (InvalidWaveRefException e) {
      LOG.warning("Cannot decode: " + waveRefStr, e);
      return null;
    }

    WaveId waveId = waveRef.getWaveId();
    WaveletId waveletId =
        waveRef.getWaveletId() != null ? waveRef.getWaveletId() : WaveletId.of(waveId.getDomain(),
            IdConstants.CONVERSATION_ROOT_WAVELET);

    WaveletName waveletName = WaveletName.of(waveId, waveletId);
    return waveletName;
  }

  private static String getWaveRefFromRequest(HttpServletRequest request) {
    String waveRefStrEncoded = request.getParameter("waveRef");
    String waveRefStr = null;
    if (waveRefStrEncoded != null) {
      try {
        waveRefStr = URLDecoder.decode(waveRefStrEncoded, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        LOG.warning("Problem decoding: " + waveRefStrEncoded, e);
      }
    }
    return waveRefStr != null ? waveRefStr : "";
  }

  private static String getMimeTypeByFileName(String fileName) {
    String mimeType = "application/octet-stream";
    if (fileName.endsWith(".ico")) {
      mimeType = "image/png;";
    } else if (fileName.endsWith(".xml")) {
      mimeType = "text/plain;";
    } else if (fileName.endsWith(".png")) {
      mimeType = "image/png;";
    } else if (fileName.endsWith(".html")) {
      mimeType = "text/html;";
    } else if (fileName.endsWith(".doc")) {
      mimeType = "application/msword;";
    } else if (fileName.endsWith(".pdf")) {
      mimeType = "application/pdf;";
    } else if (fileName.endsWith(".mp3")) {
      mimeType = "audio/mpeg;";
    } else if (fileName.endsWith(".gif")) {
      mimeType = "image/gif;";
    }
    return mimeType;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String attachmentId = getAttachmentIdFromRequest(request);
    String fileName = getFileNameFromRequest(request);
    // TODO (Yuri Z.) Add an index to map between attachment ids and the wavelet
    // names to allow retrieval of attachments just by id, without specifying
    // wavelet
    // name.
    String waveRefStr = getWaveRefFromRequest(request);
    if (attachmentId.isEmpty() || fileName.isEmpty() || waveRefStr.isEmpty()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    WaveletName waveletName = waveRef2WaveletName(waveRefStr);
    if (waveletName == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    ParticipantId user = sessionManager.getLoggedInUser(request.getSession(false));
    boolean isAuthorized = false;
    try {
      isAuthorized = waveletProvider.checkAccessPermission(waveletName, user);
    } catch (WaveServerException e) {
      LOG.warning("Problem while authorizing user: " + user + " for wavelet: " + waveletName, e);
    }
    if (!isAuthorized) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    AttachmentData data = store.getAttachment(waveletName, attachmentId);
    if (data == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    response.setContentType(getMimeTypeByFileName(fileName));
    response.setContentLength((int) data.getContentSize());
    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentLength((int) data.getContentSize());
    response.setDateHeader("Last-Modified", data.getLastModifiedDate().getTime());
    data.writeDataTo(response.getOutputStream());

    LOG.info("Fetched attachment with id '" + attachmentId + "'");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    // Process only multipart requests.
    if (ServletFileUpload.isMultipartContent(req)) {
      // Create a factory for disk-based file items.
      FileItemFactory factory = new DiskFileItemFactory();

      // Create a new file upload handler.
      ServletFileUpload upload = new ServletFileUpload(factory);

      // Parse the request.
      try {
        @SuppressWarnings("unchecked")
        List<FileItem> items = upload.parseRequest(req);
        String id = null;
        String waveRefStr = null;
        FileItem fileItem = null;
        for (FileItem item : items) {
          // Process only file upload - discard other form item types.
          if (item.isFormField()) {
            if (item.getFieldName().equals("attachmentId")) {
              id = item.getString();
            }
            if (item.getFieldName().equals("waveRef")) {
              waveRefStr = item.getString();
            }
          } else {
            fileItem = item;
          }
        }

        if (id == null) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No attachment id in request.");
          return;
        }
        if (waveRefStr == null) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No wave reference in request.");
          return;
        }

        WaveletName waveletName = waveRef2WaveletName(waveRefStr);
        ParticipantId user = sessionManager.getLoggedInUser(req.getSession(false));
        boolean isAuthorized = waveletProvider.checkAccessPermission(waveletName, user);
        if (!isAuthorized) {
          resp.sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        String fileName = fileItem.getName();
        // Get only the file name not whole path.
        if (fileName != null) {
          fileName = FilenameUtils.getName(fileName);
          if (store.storeAttachment(waveletName, id, fileItem.getInputStream())) {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            String msg =
                String.format("The file with name: %s and id: %s was created successfully.",
                    fileName, id);
            LOG.fine(msg);
            resp.getWriter().print("OK");
          } else {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            String msg = "Attachment ID " + id + " already exists!";
            LOG.warning(msg);
            resp.getWriter().print(msg);
          }
          resp.flushBuffer();
        }
      } catch (Exception e) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "An error occurred while creating the file : " + e.getMessage());
      }
    } else {
      resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
          "Request contents type is not supported by the servlet.");
    }
  }
}
