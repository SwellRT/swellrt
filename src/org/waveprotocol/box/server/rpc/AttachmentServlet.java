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

import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An attachment servlet is a simple servlet that serves up attachments from a provided store.
 */
@SuppressWarnings("serial")
@Singleton
public class AttachmentServlet extends HttpServlet {
  private static final Log LOG = Log.get(AttachmentServlet.class);

  private final AttachmentStore store;

  @Inject
  private AttachmentServlet(AttachmentStore store) {
    this.store = store;
  }

  /**
   * Get the attachment id from the URL in the request.
   * @param request
   * @return the id of the referenced attachment.
   */
  private static String getAttachmentIdFromRequest(HttpServletRequest request) {
    if (request.getPathInfo().length() == 0) {
      return "";
    }

    // Discard the leading '/' in the pathinfo. Whats left will be the attachment id.
    return request.getPathInfo().substring(1);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // TODO: Authenticate that the user has permission to access the attachment.

    String attachmentId = getAttachmentIdFromRequest(request);
    if (attachmentId.length() == 0) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    AttachmentData data = store.getAttachment(attachmentId);

    if (data == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentLength((int) data.getContentSize());
    response.setDateHeader("Last-Modified", data.getLastModifiedDate().getTime());
    data.writeDataTo(response.getOutputStream());

    LOG.info("Fetched attachment with id '" + attachmentId + "'");
  }

  @Override
  protected void doPut(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    // TODO: Authenticate the attachment data

    String attachmentId = getAttachmentIdFromRequest(request);
    if (attachmentId.length() == 0) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    store.storeAttachment(attachmentId, request.getInputStream());

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write("<html><body><h1>Data written</h1></body></html>");

    LOG.info("Added attachment with id '" + attachmentId + "'");
  }
}
