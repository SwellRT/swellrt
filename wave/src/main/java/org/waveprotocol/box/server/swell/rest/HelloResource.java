package org.waveprotocol.box.server.swell.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.waveprotocol.box.server.authentication.SessionManager;

import com.google.inject.Inject;

@Path("echo")
@Produces(MediaType.TEXT_PLAIN)
public class HelloResource {

  SessionManager sessionManager;

  @Inject
  public HelloResource(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @GET
  @Path("/{msg}")
  public Response greetings(@PathParam("msg") String msg) {
    String result = "Echo : " + msg;
    if (sessionManager != null)
      result += "\n Session Manager is ready";
    else
      result += "\n Session Manager is null";
    return Response.status(200).entity(result).build();
  }

}
