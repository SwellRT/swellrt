package org.waveprotocol.box.server.swell.rest;

import static org.waveprotocol.box.server.swell.rest.RestModule.WAVE_ID;
import static org.waveprotocol.box.server.swell.rest.RestModule.WAVE_PATH;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.NamingStore;
import org.waveprotocol.box.server.persistence.NamingStore.WaveNaming;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.swell.rest.exceptions.NoParticipantSessionException;
import org.waveprotocol.box.server.swell.rest.exceptions.WaveletAccessForbiddenException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * The naming REST resource allows to manage human friendly names for Waves.
 *
 * <p>
 * <br>
 * Responses syntax is: <code>
 * {
 *    wave_id: "...",
 *    names: [
 *      {
 *        name: "...",
 *        created: (timestamp)
 *      },
 *      ...
 *    ]
 *  }
 * </code>
 *
 * @author pablojan@gmail.com
 *
 */
@Path("naming")
@Produces(MediaType.APPLICATION_JSON)
public class NamingRestResources {

  private final NamingStore namingStore;
  private final SessionManager sessionManager;
  private final WaveletProvider waveletProvider;
  private final Gson gson = new Gson();

  @Inject
  public NamingRestResources(NamingStore namingStore, SessionManager sessionManager,
      WaveletProvider waveletProvider) {
    this.sessionManager = sessionManager;
    this.waveletProvider = waveletProvider;
    this.namingStore = namingStore;
  }


  /**
   * Query a Wave Id by name.
   *
   * @throws WaveletAccessForbiddenException
   * @throws NoParticipantSessionException
   */
  @GET
  @Path("/name/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getWaveId(
      @Context HttpServletRequest httpRequest,
      @PathParam("name") String name)
      throws WaveletAccessForbiddenException, NoParticipantSessionException {

    String result = "{}";

    WaveNaming naming;
    try {

      naming = namingStore.getWaveNamingsByName(name);
      if (naming != null) {

        ParticipantId participantId = RestUtils.getRequestParticipant(httpRequest, sessionManager);

        RestUtils.checkWaveletAccess(RestUtils.getMasterWaveletName(naming.waveId),
            waveletProvider, participantId);

        result = gson.toJson(naming);
      }

    } catch (PersistenceException e) {
      throw new IllegalStateException(e);
    }


    return Response.status(200).cacheControl(RestModule.CACHE_NO_STORE).entity(result).build();
  }



  /**
   * Get synonymous names of a Wave.
   *
   * @throws WaveletAccessForbiddenException
   * @throws NoParticipantSessionException
   */
  @GET
  @Path(WAVE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getWaveName(
      @Context HttpServletRequest httpRequest,
      @PathParam(WAVE_ID) WaveId waveId)
      throws WaveletAccessForbiddenException, NoParticipantSessionException {

    ParticipantId participantId = RestUtils.getRequestParticipant(httpRequest, sessionManager);

    RestUtils.checkWaveletAccess(RestUtils.getMasterWaveletName(waveId),
        waveletProvider, participantId);

    String result = "{}";

    WaveNaming naming = namingStore.getWaveNamingById(waveId);
    if (naming != null) {
      result = gson.toJson(naming);
    }

    return Response.status(200).cacheControl(RestModule.CACHE_NO_STORE).entity(result).build();

  }

  /**
   * Set a new name for a Wave.
   *
   */
  @POST
  @Path(WAVE_PATH + "/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response setWaveName(
      @Context HttpServletRequest httpRequest,
      @PathParam(WAVE_ID) WaveId waveId,
      @PathParam("name") String name)
      throws NoParticipantSessionException, WaveletAccessForbiddenException {

    ParticipantId participantId = RestUtils.getRequestParticipant(httpRequest, sessionManager);

    RestUtils.checkWaveletAccess(RestUtils.getMasterWaveletName(waveId),
        waveletProvider, participantId);

    String result = "{}";

    try {
      WaveNaming naming = namingStore.addWaveName(waveId, name);
      if (naming != null) {
        result = gson.toJson(naming);
      }
    } catch (PersistenceException e) {
      throw new IllegalStateException(e);
    }

    return Response.status(200).cacheControl(RestModule.CACHE_NO_STORE).entity(result).build();
  }


  /**
   * Delete a name of a Wave or all of them.
   */
  @DELETE
  @Path(WAVE_PATH + "/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteWaveName(
      @Context HttpServletRequest httpRequest,
      @PathParam(WAVE_ID) WaveId waveId,
      @PathParam("name") String name)
      throws NoParticipantSessionException, WaveletAccessForbiddenException {

    ParticipantId participantId = RestUtils.getRequestParticipant(httpRequest, sessionManager);

    RestUtils.checkWaveletAccess(RestUtils.getMasterWaveletName(waveId),
        waveletProvider, participantId);

    String result = "{}";
    WaveNaming naming = namingStore.removeWaveName(waveId, name);
    if (naming != null) {
      result = gson.toJson(naming);
    }

    return Response.status(200).cacheControl(RestModule.CACHE_NO_STORE).entity(result).build();


  }

}
