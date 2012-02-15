/*
 * Copyright 2012 A. Kaplanov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.waveprotocol.box.waveimport;

import org.waveprotocol.box.waveimport.google.RobotApi;
import org.waveprotocol.box.waveimport.google.RobotSearchDigest;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAuthorizationRequestUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.waveprotocol.wave.model.wave.ParticipantId;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.LinkedList;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.box.waveimport.google.oauth.OAuthCredentials;
import org.waveprotocol.box.waveimport.google.oauth.OAuthRequestHelper;
import org.waveprotocol.box.waveimport.google.oauth.OAuthedFetchService;
import org.waveprotocol.box.waveimport.google.oauth.UserContext;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * Export waves to files
 *
 * @author (akaplanov@gmail.com) (Andrew Kaplanov)
 */
public class WaveExport {

  private static final Logger LOG = Logger.getLogger(WaveExport.class.getName());
  private static final String AUTH_RPC = "http://wave.googleusercontent.com/api/rpc";
  private static final String WAVE_RPC = "https://www-opensocial.googleusercontent.com/api/rpc";
  private static final String AUTH_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
  private static final String SEARCH_ALL_QUERY = "after:2000/01/01 before:2012/12/31";
  private static final String FILE_NUMBER_PATTERN="000000";
  private final String clientId;
  private final String clientSecret;
  private final String participantId;
  private final String exportDir;
  private final List<String> includeList;
  private final List<String> excludeList;
  private String refreshToken;
  private String accessToken;

  public WaveExport(String clientId, String clientSecret, String participantId, String exportDir,
      List<String> includeList, List<String> excludeList) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.participantId = participantId;
    this.exportDir = exportDir;
    this.includeList = includeList;
    this.excludeList = excludeList;
  }

  static public void usageError() {
    System.err.println("Use: WaveExport <ClientId> <ClientSecret> <ParticipantId> <ExportDir>"
        + " [-i IncludeWavesList] [-e ExcludeWavesList]");
    System.exit(1);
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 4) {
      usageError();
    }
    List<String> includeList = new LinkedList<String>();
    List<String> excludeList = new LinkedList<String>();
    for (int i = 4; i < args.length;) {
      if (args[i].equals("-i")) {
        for (i = i + 1; i < args.length && args[i].charAt(0) != '-'; i++) {
          includeList.add(args[i]);
        }
      } else if (args[i].equals("-e")) {
        for (i = i + 1; i < args.length && args[i].charAt(0) != '-'; i++) {
          excludeList.add(args[i]);
        }
      } else {
        usageError();
      }
    }
    WaveExport export = new WaveExport(args[0], args[1], args[2], args[3], includeList, excludeList);
    export.authorizeToGoogle();
    export.exportWavesToFiles();
  }

  /*
   * Authorize user by OAuth on Google
   * manual http://code.google.com/apis/accounts/docs/OAuth2InstalledApp.html
   * example http://code.google.com/p/google-api-java-client/wiki/OAuth2Draft10
   */
  public void authorizeToGoogle() throws IOException {
    // Generate the URL to which we will direct users
    GoogleAuthorizationRequestUrl authUrl = new GoogleAuthorizationRequestUrl(
        clientId, AUTH_REDIRECT_URI, AUTH_RPC);
    String authorizeUrl = authUrl.build();
    System.out.println("Paste this URL in your browser:\n" + authorizeUrl);

    // Wait for the authorization code
    System.out.println("Type the code you received here: ");
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String authorizationCode = in.readLine();

    HttpTransport transport = new NetHttpTransport();
    JsonFactory factory = new JacksonFactory();

    // Exchange for an access and refresh token
    GoogleAuthorizationCodeGrant authRequest = new GoogleAuthorizationCodeGrant(transport,
        factory, clientId, clientSecret, authorizationCode, AUTH_REDIRECT_URI);
    authRequest.useBasicAuthorization = false;
    AccessTokenResponse authResponse = authRequest.execute();

    accessToken = authResponse.accessToken;
    refreshToken = authResponse.refreshToken;

    GoogleAccessProtectedResource access = new GoogleAccessProtectedResource(accessToken, transport,
        factory, clientId, clientSecret, refreshToken);
    access.refreshToken();
  }

  /*
   * Export waves to files
   */
  public void exportWavesToFiles() {
    HttpClient httpClient = new HttpClient();
    UserContext context = new UserContext();
    context.setParticipantId(new ParticipantId(participantId));
    OAuthCredentials cred = new OAuthCredentials(refreshToken, accessToken);
    context.setOAuthCredentials(cred);
    OAuthRequestHelper helper = new OAuthRequestHelper(clientId, clientSecret, context);
    OAuthedFetchService oauthService = new OAuthedFetchService(httpClient, helper);
    RobotApi api = new RobotApi(oauthService, WAVE_RPC);
    try {
      List<String> waves = includeList;
      if (waves.isEmpty()) {
        waves = getAllWavesList(api);
      }
      int processedCount = 0;
      int errorCount = 0;
      for (String waveId : waves) {
        try {
          if (excludeList.contains(waveId)) {
            System.out.println("Skipping wave " + waveId + "...");
            continue;
          }
          for (WaveletId waveletId : api.getWaveView(WaveId.deserialise(waveId))) {
            long fromVersion = 0;
            for (int fetchNum = 0;; fetchNum++) {
              System.out.println("Exporting wavelet " + waveId + ":" + waveletId.serialise()
                  + " from version " + fromVersion + " ...");
              JSONObject json = api.fetchWaveWithDeltas(WaveId.deserialise(waveId),
                  waveletId, fromVersion);
              JSONArray rawDeltas = json.getJSONObject("data").getJSONArray("rawDeltas");
              if (rawDeltas.length() == 0) {
                break;
              }
              ProtocolWaveletDelta firstDelta = ProtocolWaveletDelta.parseFrom(
                  ProtocolAppliedWaveletDelta.parseFrom(
                  Base64.decodeBase64(rawDeltas.getString(0).getBytes())).
                  getSignedOriginalDelta().getDelta());
              ProtocolWaveletDelta lastDelta = ProtocolWaveletDelta.parseFrom(
                  ProtocolAppliedWaveletDelta.parseFrom(
                  Base64.decodeBase64(rawDeltas.getString(rawDeltas.length() - 1).getBytes())).
                  getSignedOriginalDelta().getDelta());
              if (firstDelta.getHashedVersion().getVersion() != fromVersion) {
                if (lastDelta.getHashedVersion().getVersion() == fromVersion - 1) {
                  break;
                }
                System.err.println("Error : expected version " + fromVersion
                    + ", got version " + firstDelta.getHashedVersion().getVersion());
                errorCount++;
                break;
              }
              String fileName = exportDir + "/"
                  + FileUtils.waveIdToPathSegment(WaveId.deserialise(waveId)) + "."
                  + FileUtils.waveletIdToPathSegment(waveletId) + "."
                  + new DecimalFormat(FILE_NUMBER_PATTERN).format(fetchNum) + ".json";
              writeFile(fileName, json.toString());
              fromVersion = lastDelta.getHashedVersion().getVersion() + 1;
            }
          }
          processedCount++;
        } catch (Exception ex) {
          errorCount++;
          LOG.log(Level.SEVERE, null, ex);
          System.out.println("Error " + ex.toString());
        }
      }
      System.out.println("Processed count " + processedCount);
      System.out.println("Error count " + errorCount);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      System.err.println(ex.toString());
    }
  }

  private List<String> getAllWavesList(RobotApi api) throws IOException {
    List<String> allList = new LinkedList<String>();
    for (int i = 0;;) {
      List<RobotSearchDigest> digests = api.search(SEARCH_ALL_QUERY, i, 100);
      if (digests.isEmpty()) {
        break;
      }
      i += digests.size();
      for (RobotSearchDigest digest : digests)
        allList.add(digest.getWaveId());
    }
    return allList;
  }

  static private void writeFile(String name, String data) throws IOException {
    FileWriter w = new FileWriter(new File(name));
    w.write(data);
    w.close();
  }
}
