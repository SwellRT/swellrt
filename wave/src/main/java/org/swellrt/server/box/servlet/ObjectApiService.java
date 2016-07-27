package org.swellrt.server.box.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.swellrt.model.generic.Model;
import org.swellrt.server.box.objectapi.ObjectApi;
import org.swellrt.server.box.objectapi.ObjectApiException;
import org.swellrt.server.box.objectapi.OpRecorderWavelet;
import org.waveprotocol.box.common.comms.WaveClientRpc;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.frontend.ClientFrontend;
import org.waveprotocol.box.server.frontend.ClientFrontend.OpenListener;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;
import org.waveprotocol.wave.util.logging.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * A REST service to edit swellrt collaborative objects. (Draft version)
 * 
 * <pre>
 * <code>
 * POST /swell/object/waveid/path
 * { json }
 * </code>
 * </pre>
 * 
 * <code>path</code> uses "/" as separator.
 * <br/>
 * TODO implement GET and DELETE operations
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ObjectApiService extends BaseService {

	private static final Log LOG = Log.get(ObjectApiService.class);
	
	private final WaveletProvider waveletProvider;
	private final IdURIEncoderDecoder decoder = new IdURIEncoderDecoder(new JavaUrlCodec());

	@Inject
	public ObjectApiService(SessionManager sessionManager, Config config, WaveletProvider waveletProvider) {
		super(sessionManager);
		this.waveletProvider = waveletProvider;
	}

	@Override
	public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {
		
		try {
			

			
			ParticipantId participantId = getLoggedInUser(req);	
					
			String requestPath = SwellRtServlet.getCleanPathInfo(req);			
			WaveletName waveletName = extractWaveletName(requestPath);
			
			String path = extractObjectPath(requestPath);
			String method = req.getMethod();
			
			if (!method.equalsIgnoreCase("POST") || !method.equalsIgnoreCase("GET") ) {
				new ServiceException("Invalid HTTP method",  HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_HTTP_METHOD);
			}
			
			CommittedWaveletSnapshot snapshot = waveletProvider.getSnapshot(waveletName);
			OpRecorderWavelet recorderWavelet = new OpRecorderWavelet(snapshot.snapshot, snapshot.committedVersion, participantId);

			Model model = Model.create(recorderWavelet.getWavelet(), participantId, new IdGeneratorImpl.Seed() {

				@Override
				public String get() {
					return req.getSession().getId();
				}

			});

			JsonParser jsonParser = new JsonParser();
			JsonElement jsonBody = null;
			
			if (method.equalsIgnoreCase("POST")) {
				jsonBody = jsonParser.parse(new InputStreamReader(req.getInputStream()));
				ObjectApi.doUpdate(model, path, jsonBody);
				
			} else if (method.equalsIgnoreCase("GET")) {
				JsonElement json = ObjectApi.doGet(model, path);
				response.setContentType("application/json");
				sendResponse(response, json);
				
			} else if (method.equalsIgnoreCase("DELETE")) {
				// TODO implement
			}
				

			for (WaveletDelta delta : recorderWavelet.getDeltas()) {
				ProtocolWaveletDelta protocolDelta = CoreWaveletOperationSerializer.serialize(delta);

				waveletProvider.submitRequest(waveletName, protocolDelta, new WaveletProvider.SubmitRequestListener() {

					@Override
					public void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
							long applicationTimestamp) {
						LOG.info("Object Operation ("+method+","+requestPath+") applied "+operationsApplied+" operations, resulting version "+hashedVersionAfterApplication.getVersion()+" by "+participantId.getAddress());

					}

					@Override
					public void onFailure(String errorMessage) {
						LOG.info("Object Operation error ("+method+","+requestPath+") "+errorMessage);
					}
				});

			}


		} catch (ServiceException e) {
			 sendResponseError(response, e.httpResponseCode, e.getServiceResponseCode());
		} catch (JsonParseException e) {
			sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_JSON_SYNTAX);
		} catch (ObjectApiException e) {
			sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getCode());
		} catch (Exception e) {
			sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RC_INTERNAL_SERVER_ERROR);
		}
		
	}
	
	
	private String extractObjectPath(String requestPath) throws ServiceException {
		try {
			String[] parts = requestPath.split("/");
			String path = "";
			for (int i = 4; i < parts.length; i++) {
				if (!path.isEmpty()) {
					path += ".";
				}
				path += parts[i];
			}
			return path;
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_OBJECT_PATH);
		}
	}
	
	
	private WaveletName extractWaveletName(String requestPath) throws ServiceException {
		try {
			String[] pathParts = requestPath.split("/");		
			String serialWaveletName = pathParts[2]+ "/" + pathParts[3] + "/" + Model.WAVELET_SWELL_ROOT;		
			return decoder.uriPathToWaveletName(serialWaveletName);
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_OBJECT_ID);
		}
	}
	
	
	
}
