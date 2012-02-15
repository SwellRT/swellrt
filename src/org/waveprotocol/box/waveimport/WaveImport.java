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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * Import waves from files to Wiab
 *
 * @author (akaplanov@gmail.com) (Andrew Kaplanov)
 */
public final class WaveImport {

  private final String waveServerImportUrl;
  private final String waveServerDomain;
  private final String exportDir;

  private WaveImport(String waveServerImportUrl, String waveServerDomain, String exportDir) {
    this.waveServerImportUrl = waveServerImportUrl;
    this.waveServerDomain = waveServerDomain;
    this.exportDir = exportDir;
  }

  public static void main(String[] args) {
    if (args.length != 3) {
      System.err.println("Use: WaveImport <WaveServerImportUrl> <WaveServerDomain> <ExportDir>");
      System.exit(1);
    }
    new WaveImport(args[0], args[1], args[2]).importWavesFromFiles();
  }

  /*
   * Import waves from files
   */
  public void importWavesFromFiles() {
    File dir = new File(exportDir);
    if (!dir.exists()) {
      System.err.println("Directory " + exportDir + " is not exists.");
      System.exit(1);
    }
    File[] files = dir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File file, String name) {
        return name.endsWith("json");
      }
    });
    Arrays.sort(files, new Comparator<File>() {

      @Override
      public int compare(File f1, File f2) {
        return f1.getName().compareTo(f2.getName());
      }
    });
    int importedCount = 0;
    int appendedCount = 0;
    int skippedCount = 0;
    int errorCount = 0;
    for (int i = 0; i < files.length; i++) {
      try {
        File file = files[i];
        System.out.println("Importing " + file + " (" + (i + 1) + " of " + files.length + ") ...");
        StringTokenizer fileNameTokenizer = new StringTokenizer(file.getName(), ".");
        WaveId waveId = FileUtils.waveIdFromPathSegment(fileNameTokenizer.nextToken());
        waveId = WaveId.of(waveServerDomain, waveId.getId());
        WaveletId waveletId = FileUtils.waveletIdFromPathSegment(fileNameTokenizer.nextToken());
        waveletId = WaveletId.of(waveServerDomain, waveletId.getId());
        String reply = importRequest(waveServerImportUrl, waveId, waveletId, readFile(file));
        System.out.println("... " + reply);
        StringTokenizer replyTokenizer = new StringTokenizer(reply);
        String status = replyTokenizer.nextToken();
        if (status.equals("imported")) {
          importedCount++;
        } else if (status.equals("appended")) {
          appendedCount++;
        } else if (status.equals("error")) {
          errorCount++;
        } else if (status.equals("skipped")) {
          skippedCount++;
        }
      } catch (IOException ex) {
        errorCount++;
        ex.printStackTrace(System.err);
      }
    }
    System.out.println("Imported count " + importedCount);
    System.out.println("Appended count " + appendedCount);
    System.out.println("Skipped count " + skippedCount);
    System.out.println("Error count " + errorCount);
  }

  private String importRequest(String url, WaveId waveId, WaveletId waveletId, String json)
      throws IOException {
    HttpClient httpClient = new HttpClient();
    PostMethod request = new PostMethod(url);
    request.setRequestHeader("Content-Type", "application/json; charset=UTF-8");
    request.setRequestHeader("domain", waveServerDomain);
    request.setRequestHeader("waveId", waveId.serialise());
    request.setRequestHeader("waveletId", waveletId.serialise());
    request.setRequestEntity(new ByteArrayRequestEntity(json.getBytes("utf8")));
    if (httpClient.executeMethod(request) != 200) {
      throw new IOException(request.getResponseBodyAsString());
    }
    return request.getResponseBodyAsString();
  }

  private static String readFile(File file) throws FileNotFoundException, IOException {
    Reader reader = new FileReader(file);
    StringBuilder sb = new StringBuilder();
    char buf[] = new char[1000];
    for (;;) {
      int ret = reader.read(buf, 0, buf.length);
      if (ret == -1) {
        break;
      }
      sb.append(buf, 0, ret);
    }
    return sb.toString();
  }
}
