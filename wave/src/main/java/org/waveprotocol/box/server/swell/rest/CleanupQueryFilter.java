package org.waveprotocol.box.server.swell.rest;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.waveprotocol.wave.util.logging.Log;



@Provider
@PreMatching
/**
 * Remove session parameters from query string like one from
 * {@link TransientIdFilter}
 *
 * <pre>
 * ;tid=...;sid=...
 * </pre>
 *
 * @author pablojan@gmail.com
 *
 */
public class CleanupQueryFilter implements ContainerRequestFilter {

  private static final Log LOG = Log.get(CleanupQueryFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

    URI uri = requestContext.getUriInfo().getRequestUri();
    String uriStr = uri.toString();
    int separatorAt = uriStr.indexOf(";");
    if (separatorAt != -1) {
      // remove ";tid=...;sid=..." parts from end of URI.
      uriStr = uriStr.substring(0, uriStr.indexOf(";"));
      requestContext.setRequestUri(URI.create(uriStr));
    }

    LOG.finest(requestContext.getUriInfo().getRequestUri().toString());
  }

}
