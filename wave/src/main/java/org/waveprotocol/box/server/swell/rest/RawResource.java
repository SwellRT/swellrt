package org.waveprotocol.box.server.swell.rest;

import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;


/**
 * The Raw REST resource provides access to wavelet raw data, versions, etc.
 *
 * @author pablojan@gmail.com
 *
 */
@Path("raw")
@Produces(MediaType.APPLICATION_JSON)
public class RawResource {

  HashedVersionFactory HashVersionFactory = new HashedVersionZeroFactoryImpl(
      new IdURIEncoderDecoder(new JavaUrlCodec()));


  private static final String WAVE_ID = "waveid";
  private static final String WAVELET_ID = "waveletid";
  private static final String DOC_ID = "docid";

  private static final String VERSION_START = "vstart";
  private static final String VERSION_END = "vend";
  private static final String NUM_OF_RESULTS = "limit";
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
  public RawResource(SessionManager sessionManager, WaveletProvider waveletProvider) {
    this.sessionManager = sessionManager;
    this.waveletProvider = waveletProvider;
  }

  /**
   * Returns history log of a document.
   */
  @GET
  @Path(DOC_PATH + "/log")
  public Response documentLog(@PathParam(WAVE_ID) WaveId waveId,
      @PathParam(WAVELET_ID) WaveletId waveletId, @PathParam(DOC_ID) String docId,
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
            start = HashVersionFactory.createVersionZero(waveletName);
          }

          HashedVersion end = versionEnd;
          if (end == null) {
            CommittedWaveletSnapshot committedSnaphot = waveletProvider.getSnapshot(waveletName);
            end = committedSnaphot.snapshot.getHashedVersion();
          }

          DocumentLog.build(waveletProvider, waveletName, docId, start, end, numberOfResults,
              jw, returnOperations);

        } catch (WaveServerException e) {
          throw new IllegalStateException(e);
        }

      }

    };

    return Response.status(200).cacheControl(CACHE_24H).entity(response).build();
  }


  /**
   * Returns history log of a document.
   */
  @GET
  @Path(DOC_PATH + "/content/{version}")
  public Response documentContent(@PathParam(WAVE_ID) WaveId waveId,
      @PathParam(WAVELET_ID) WaveletId waveletId,
      @PathParam(DOC_ID) String docId,
      @PathParam(VERSION_PATH_SEGMENT) String version) {

    return null;
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
