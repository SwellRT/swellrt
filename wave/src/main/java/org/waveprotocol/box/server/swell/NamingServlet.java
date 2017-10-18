package org.waveprotocol.box.server.swell;

import java.io.IOException;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.swellrt.beta.model.wave.WaveCommons;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.NamingStore;
import org.waveprotocol.box.server.persistence.NamingStore.WaveNaming;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * Service to map text names with wave identifiers. Names are URL safe strings.
 * <p>
 * <br>
 * GET /naming/name/{name}
 * <p>
 * <br>
 * Get the wave id associated with a name, plus other synonymous, error
 * otherwise
 * <p>
 * <br>
 * GET /naming/wave/{waveDomain}/{waveId}
 * <p>
 * <br>
 * Get the names synonymous mapped with this wave id. Throw error if wave
 * doesn't exist.
 * <p>
 * <br>
 * POST /naming/wave/{waveDomain}/{waveId}/{name}
 * <p>
 * <br>
 * Map a new name with a provided wave id
 * <p>
 * <br>
 * DELETE /naming/wave/{waveDomain}/{waveId}/{name}
 * <p>
 * <br>
 * Delete name from wave id mapping
 * <p>
 * <br>
 * DELETE /naming/wave/{waveDomain}/{waveId}
 * <p>
 * <br>
 * Delete all mappings for wave id
 * <p>
 * <br>
 * <p>
 * <br>
 * Format for GET responses is <br>
 * <code>
 * {
 *    wave_id: "(wave id)",
 *    names: [
 *      {
 *        name: "a url safe name",
 *        created: (timestamp)
 *      },
 *      ...
 *    ]
 *  }
 * </code>
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@SuppressWarnings("serial")
@Singleton
public class NamingServlet extends HttpServlet {

  private static final Log LOG = Log.get(NamingServlet.class);

  public static final String SERVLET_URL_PATTERN = "/naming/*";

  private final NamingStore store;
  private final SessionManager sessionManager;
  private final WaveletProvider waveletProvider;
  private final Gson gson;

  @Inject
  public NamingServlet(NamingStore store, SessionManager sessionManager,
      WaveletProvider waveletProvider) {
    this.store = store;
    this.sessionManager = sessionManager;
    this.waveletProvider = waveletProvider;
    this.gson = new Gson();
  }


  @Override
  @VisibleForTesting
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req);
    if (participantId == null) {
      ServletUtils.responseForbidden(response, "Not logged in user");
      return;
    }

    Map<String, String> params = ServletUtils.getUrlParams(req,
        Lists.newArrayList("entityToken", "token1", "token2"));

    if (!params.containsKey("entityToken")) {
      ServletUtils.responseJsonEmpty(response);
      return;
    }

    if (params.get("entityToken").equals("name")) {

      if (params.containsKey("token1")) {

        try {
          WaveNaming naming = store.getWaveNamingsByName(params.get("token1"));
          if (naming == null) {
            ServletUtils.responseJsonEmpty(response);
            return;
          }

          waveletProvider.checkAccessPermission(
              WaveletName.of(naming.waveId,
                  WaveletId.of(naming.waveId.getDomain(), WaveCommons.MASTER_DATA_WAVELET_NAME)),
              participantId);

          ServletUtils.responseJson(response, gson.toJson(naming), ServletUtils.NO_CACHE);

        } catch (PersistenceException e) {
          ServletUtils.responseInternalError(response, e.getMessage());

        } catch (WaveServerException e) {
          ServletUtils.responseForbidden(response, e.getMessage());

        }
      } else {
        ServletUtils.responseJsonEmpty(response);
      }

      return;

    } else if (params.get("entityToken").equals("wave")) {

      String waveDomain = params.get("token1");
      String waveIdStr = params.get("token2");

      try {
        WaveId waveId = WaveId.ofChecked(waveDomain, waveIdStr);

        waveletProvider.checkAccessPermission(
            WaveletName.of(waveId,
                WaveletId.of(waveId.getDomain(), WaveCommons.MASTER_DATA_WAVELET_NAME)),
            participantId);

        WaveNaming naming = store.getWaveNamingById(waveId);
        ServletUtils.responseJson(response, gson.toJson(naming), ServletUtils.NO_CACHE);

      } catch (InvalidIdException e) {
        ServletUtils.responseBadRequest(response, "Invalid Wave id");
      } catch (WaveServerException e) {
        ServletUtils.responseForbidden(response, e.getMessage());
      }

      return;
    }

    ServletUtils.responseBadRequest(response, "Invalid request syntax");

  }

  @Override
  @VisibleForTesting
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req);
    if (participantId == null) {
      ServletUtils.responseForbidden(response, "Not logged in user");
      return;
    }

    Map<String, String> params = ServletUtils.getUrlParams(req,
        Lists.newArrayList("waveToken", "waveDomain", "waveId", "name"));

    if (!params.containsKey("waveToken") || !params.get("waveToken").equals("wave")) {
      ServletUtils.responseBadRequest(response, "no /wave/ path element found");
      return;
    }

    String waveDomain = params.get("waveDomain");
    String waveIdStr = params.get("waveId");
    String name = params.get("name");

    if (name == null) {
      ServletUtils.responseBadRequest(response, "Invalid name");
      return;
    }

    try {
      WaveId waveId = WaveId.ofChecked(waveDomain, waveIdStr);

      waveletProvider.checkAccessPermission(
          WaveletName.of(waveId,
              WaveletId.of(waveId.getDomain(), WaveCommons.MASTER_DATA_WAVELET_NAME)),
          participantId);

      WaveNaming naming = store.addWaveName(waveId, name);
      ServletUtils.responseJson(response, gson.toJson(naming), ServletUtils.NO_CACHE);

    } catch (InvalidIdException e) {
      ServletUtils.responseBadRequest(response, "Invalid Wave id");
    } catch (PersistenceException e) {
      ServletUtils.responseInternalError(response, e.getMessage());
    } catch (WaveServerException e) {
      ServletUtils.responseForbidden(response, e.getMessage());
    }
  }

  @Override
  @VisibleForTesting
  protected void doDelete(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req);
    if (participantId == null) {
      ServletUtils.responseForbidden(response, "Not logged in user");
      return;
    }

    Map<String, String> params = ServletUtils.getUrlParams(req,
        Lists.newArrayList("waveToken", "waveDomain", "waveId", "name"));

    if (!params.containsKey("waveToken") || !params.get("waveToken").equals("wave")) {
      ServletUtils.responseBadRequest(response, "no /wave/ path element found");
      return;
    }

    String waveDomain = params.get("waveDomain");
    String waveIdStr = params.get("waveId");
    String name = params.get("name");

    if (name == null) {
      ServletUtils.responseBadRequest(response, "Invalid name");
      return;
    }

    try {
      WaveId waveId = WaveId.ofChecked(waveDomain, waveIdStr);

      waveletProvider.checkAccessPermission(
          WaveletName.of(waveId,
              WaveletId.of(waveId.getDomain(), WaveCommons.MASTER_DATA_WAVELET_NAME)),
          participantId);

      WaveNaming naming = store.removeWaveName(waveId, name);
      ServletUtils.responseJson(response, gson.toJson(naming), ServletUtils.NO_CACHE);

    } catch (InvalidIdException e) {
      ServletUtils.responseBadRequest(response, "Invalid Wave id");
    } catch (WaveServerException e) {
      ServletUtils.responseForbidden(response, e.getMessage());
    }
  }

}
