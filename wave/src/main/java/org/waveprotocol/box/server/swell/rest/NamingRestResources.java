package org.waveprotocol.box.server.swell.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
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

import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;


/**
 * The naming REST resource allows to manage human friendly names for Waves.
 *
 * @author pablojan@gmail.com
 *
 */
@Path("naming")
@Produces(MediaType.APPLICATION_JSON)
public class NamingRestResources {


  private static final String WAVE_ID = "waveid";
  private static final String WAVELET_ID = "waveletid";
  private static final String DOC_ID = "docid";

  private static final String VERSION = "v";
  private static final String VERSION_START = "vs";
  private static final String VERSION_END = "ve";
  private static final String NUM_OF_RESULTS = "l";
  private static final String RETURN_OPS = "ops";

  private static final String VERSION_PATH_SEGMENT = "{version}";
  private static final String WAVE_PATH_SEGMENT = "wave/{waveid:.*/.*}";
  private static final String WAVELET_PATH_SEGMENT = "wavelet/{waveletid:.*/.*}";
  private static final String DOC_PATH_SEGMENT = "doc/{docid}";

  private static final String DOC_PATH = "/" + WAVE_PATH_SEGMENT + "/" + WAVELET_PATH_SEGMENT + "/"
      + DOC_PATH_SEGMENT;
  private static final String WAVELET_PATH = "/" + WAVE_PATH_SEGMENT + "/" + WAVELET_PATH_SEGMENT;
  private static final String WAVE_PATH = "/" + WAVE_PATH_SEGMENT;

  private static final CacheControl CACHE_24H = new CacheControl();
  private static final CacheControl CACHE_NO_STORE = new CacheControl();

  static {
    CACHE_24H.setMaxAge(86400);
    CACHE_NO_STORE.setNoStore(true);
  }

  private final SessionManager sessionManager;
  private final WaveletProvider waveletProvider;

  @Inject
  public NamingRestResources(SessionManager sessionManager, WaveletProvider waveletProvider) {
    this.sessionManager = sessionManager;
    this.waveletProvider = waveletProvider;
  }


  /**
   * Returns history log of a document.
   */
  @GET
  @Path(DOC_PATH + "/log")
  @Produces(MediaType.APPLICATION_JSON)
  public Response documentLog(
      @PathParam(WAVE_ID) WaveId waveId,
      @PathParam(WAVELET_ID) WaveletId waveletId,
      @PathParam(DOC_ID) String docId,
      final @QueryParam(VERSION_START) HashedVersion versionStart,
      final @QueryParam(VERSION_END) HashedVersion versionEnd,
      final @QueryParam(NUM_OF_RESULTS) int numberOfResults,
      final @QueryParam(RETURN_OPS) @DefaultValue("false") boolean returnOperations) {

    final WaveletName waveletName = WaveletName.of(waveId, waveletId);

    JsonStreamingResponse response = new JsonStreamingResponse() {

      @Override
      public void write(JsonWriter jw) throws IOException {

        try {

          HashedVersion start = versionStart;
          if (start == null) {
            start = RestUtils.HashVersionFactory.createVersionZero(waveletName);
          }

          HashedVersion end = versionEnd;
          if (end == null) {
            CommittedWaveletSnapshot committedSnaphot = waveletProvider.getSnapshot(waveletName);
            end = committedSnaphot.snapshot.getHashedVersion();
          }

          DocumentLogBuilder.build(waveletProvider, waveletName, docId, start, end, numberOfResults,
              jw, returnOperations);

        } catch (WaveServerException e) {
          throw new IllegalStateException(e);
        }

      }

    };

    return Response.status(200).cacheControl(CACHE_24H).entity(response).build();
  }



  /**
   * Returns document's content.
   */
  @GET
  @Path(DOC_PATH + "/content")
  @Produces(MediaType.APPLICATION_XML)
  public Response documentContent(
      @PathParam(WAVE_ID) WaveId waveId,
      @PathParam(WAVELET_ID) WaveletId waveletId,
      @PathParam(DOC_ID) String docId,
      @QueryParam(VERSION) HashedVersion version) {

    final WaveletName waveletName = WaveletName.of(waveId, waveletId);

    try {
      String docXML = DocumentContentBuilder.build(waveletProvider, waveletName, docId, version);
      return Response.status(200).cacheControl(CACHE_24H).entity(docXML).build();
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

    // ParticipantId participantId =
    // RestUtils.getRequestParticipant(httpRequest, sessionManager);
    // RestUtils.checkWaveletAccess(waveletName, waveletProvider,
    // participantId);

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

    return Response.status(200).cacheControl(CACHE_24H).entity(response).build();
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
