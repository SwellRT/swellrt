package org.waveprotocol.box.server.swell.rest;

import static org.waveprotocol.box.server.swell.rest.RestModule.DOC_ID;
import static org.waveprotocol.box.server.swell.rest.RestModule.DOC_PATH;
import static org.waveprotocol.box.server.swell.rest.RestModule.GROUPBY_TIME;
import static org.waveprotocol.box.server.swell.rest.RestModule.GROUPBY_USER;
import static org.waveprotocol.box.server.swell.rest.RestModule.NUM_OF_RESULTS;
import static org.waveprotocol.box.server.swell.rest.RestModule.RETURN_OPS;
import static org.waveprotocol.box.server.swell.rest.RestModule.SORT;
import static org.waveprotocol.box.server.swell.rest.RestModule.VERSION;
import static org.waveprotocol.box.server.swell.rest.RestModule.VERSION_END;
import static org.waveprotocol.box.server.swell.rest.RestModule.VERSION_START;
import static org.waveprotocol.box.server.swell.rest.RestModule.WAVELET_ID;
import static org.waveprotocol.box.server.swell.rest.RestModule.WAVELET_PATH;
import static org.waveprotocol.box.server.swell.rest.RestModule.WAVE_ID;
import static org.waveprotocol.box.server.swell.rest.RestModule.WAVE_PATH;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.swell.rest.exceptions.NoParticipantSessionException;
import org.waveprotocol.box.server.swell.rest.exceptions.WaveletAccessForbiddenException;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;


/**
 * The Raw REST resource provides access to wavelet raw data, versions, etc.
 *
 * @author pablojan@gmail.com
 *
 */
@Path("data")
@Produces(MediaType.APPLICATION_JSON)
public class DataRestResources {


  private final SessionManager sessionManager;
  private final WaveletProvider waveletProvider;

  @Inject
  public DataRestResources(SessionManager sessionManager, WaveletProvider waveletProvider) {
    this.sessionManager = sessionManager;
    this.waveletProvider = waveletProvider;
  }


  /**
   * Returns history log of a document.
   */
  @GET
  @Path(RestModule.DOC_PATH + "/log")
  @Produces(MediaType.APPLICATION_JSON)
  public Response documentLog(
      @Context HttpServletRequest httpRequest,
      @PathParam(WAVE_ID) WaveId waveId,
      @PathParam(WAVELET_ID) WaveletId waveletId,
      @PathParam(DOC_ID) String docId,
      final @QueryParam(VERSION_START) HashedVersion versionStart,
      final @QueryParam(VERSION_END) HashedVersion versionEnd,
      final @QueryParam(NUM_OF_RESULTS) int numberOfResults,
      final @QueryParam(RETURN_OPS) @DefaultValue("false") boolean returnOperations,
      final @QueryParam(SORT) @DefaultValue("asc") String sort,
      final @QueryParam(GROUPBY_TIME) @DefaultValue("0") long groupByTime,
      final @QueryParam(GROUPBY_USER) @DefaultValue("false") boolean groupByUser)

      throws NoParticipantSessionException, WaveletAccessForbiddenException {

    final WaveletName waveletName = WaveletName.of(waveId, waveletId);

    ParticipantId participantId = RestUtils.getRequestParticipant(httpRequest, sessionManager);
    RestUtils.checkWaveletAccess(waveletName, waveletProvider, participantId);

    JsonStreamingResponse response = new JsonStreamingResponse() {

      @Override
      public void write(JsonWriter jw) throws IOException {

        try {

          HashedVersion start = versionStart;
          HashedVersion end = versionEnd;

          // general case, ascending sort

          if (start == null) {
            start = RestUtils.HashVersionFactory.createVersionZero(waveletName);
          }

          if (end == null) {
            CommittedWaveletSnapshot committedSnaphot = waveletProvider.getSnapshot(waveletName);
            end = committedSnaphot.snapshot.getHashedVersion();
          }

          if (start.getVersion() > end.getVersion()) {
            throw new IllegalStateException("Start version must be less than end version");
          }

          if (sort.equals("des")) {
            HashedVersion tmp = end;
            end = start;
            start = tmp;
          }

          jw.beginObject();
          jw.name("log");

          if (groupByUser || groupByTime > 0) {

            DocumentLogBuilder.queryGroupBy(jw, waveletProvider, waveletName, docId, start, end,
                returnOperations, groupByUser, groupByTime,
                numberOfResults == 0 ? 1 : numberOfResults);

          } else {

            DocumentLogBuilder.queryAll(jw, waveletProvider, waveletName, docId, start, end,
                numberOfResults, returnOperations);
          }

          jw.endObject();

        } catch (WaveServerException e) {
          throw new IllegalStateException(e);
        }

      }

    };

    return Response.status(200).cacheControl(RestModule.CACHE_24H).entity(response).build();
  }



  /**
   * Returns document's content.
   */
  @GET
  @Path(DOC_PATH + "/content")
  @Produces(MediaType.APPLICATION_XML)
  public Response documentContent(
      @Context HttpServletRequest httpRequest,
      @PathParam(WAVE_ID) WaveId waveId,
      @PathParam(WAVELET_ID) WaveletId waveletId,
      @PathParam(DOC_ID) String docId,
      @QueryParam(VERSION) HashedVersion version)
      throws WaveletAccessForbiddenException, NoParticipantSessionException {

    final WaveletName waveletName = WaveletName.of(waveId, waveletId);

    ParticipantId participantId = RestUtils.getRequestParticipant(httpRequest, sessionManager);
    RestUtils.checkWaveletAccess(waveletName, waveletProvider, participantId);

    try {
      String docXML = DocumentContentBuilder.build(waveletProvider, waveletName, docId, version);
      return Response.status(200).cacheControl(RestModule.CACHE_24H).entity(docXML).build();
    } catch (WaveServerException e) {
      throw new IllegalStateException(e);
    }
  }


  @GET
  @Path(WAVELET_PATH + "/contrib")
  @Produces(MediaType.APPLICATION_JSON)
  public Response waveletContributions(
      @Context HttpServletRequest httpRequest,
      @PathParam(WAVE_ID) WaveId waveId,
      @PathParam(WAVELET_ID) WaveletId waveletId,
      @QueryParam(VERSION) HashedVersion version)
      throws NoParticipantSessionException, WaveletAccessForbiddenException {

    final WaveletName waveletName = WaveletName.of(waveId, waveletId);

    ParticipantId participantId = RestUtils.getRequestParticipant(httpRequest, sessionManager);
    RestUtils.checkWaveletAccess(waveletName, waveletProvider, participantId);

    JsonStreamingResponse response = new JsonStreamingResponse() {

      @Override
      public void write(JsonWriter jw) throws IOException {
        try {
          WaveletContributionsBuilder.build(waveletProvider, waveletName, version, jw);
        } catch (WaveServerException e) {
          throw new IllegalStateException(e);
        }
      }

    };

    return Response.status(200).cacheControl(RestModule.CACHE_24H).entity(response).build();
  }

  /**
   * Returns info about the wave. Just an example of JAX-RS usage.
   */
  @GET
  @Path(WAVE_PATH)
  public Response wave(@PathParam(WAVE_ID) WaveId waveId) {


    JsonStreamingResponse response = new JsonStreamingResponse() {

      @Override
      public void write(JsonWriter jw) throws IOException {

        jw.beginObject();
        jw.name("wavelets");
        jw.beginArray();

        try {
          for (WaveletId id : waveletProvider.getWaveletIds(waveId)) {
            jw.value(id.newSerialise());
          }
        } catch (WaveServerException e) {
          e.printStackTrace();
        }

        jw.endArray();
        jw.endObject();

      }

    };

    return Response.status(200).entity(response).build();
  }

}
