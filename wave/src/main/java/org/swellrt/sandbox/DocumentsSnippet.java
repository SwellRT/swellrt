package org.swellrt.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.parser.XmlParseException;

public class DocumentsSnippet {

  public String loadWaveDocument(String documentName) throws IOException {

    InputStream inputStream = this.getClass().getClassLoader()
        .getResourceAsStream(documentName);
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    // StandardCharsets.UTF_8.name() > JDK 7
    return result.toString(StandardCharsets.UTF_8.name());
  }

  public DocInitialization getAsDocInitialization(String documentName)
      throws IOException, XmlParseException {

    String docString = loadWaveDocument(documentName);
    return DocOpUtil.docInitializationFromXml(docString);
  }


  public static void main(String[] args) throws IOException, XmlParseException {

    DocumentsSnippet snippet = new DocumentsSnippet();
    snippet.getAsDocInitialization("org/swellrt/sandbox/document-1.xml");
  }

}
