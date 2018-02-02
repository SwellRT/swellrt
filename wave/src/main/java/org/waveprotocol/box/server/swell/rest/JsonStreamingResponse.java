package org.waveprotocol.box.server.swell.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import com.google.gson.stream.JsonWriter;


public abstract class JsonStreamingResponse implements StreamingOutput {

  @Override
  public final void write(OutputStream output) throws IOException, WebApplicationException {
    JsonWriter jw = new JsonWriter(new PrintWriter(output));
    write(jw);
    jw.flush();
  }

  public abstract void write(JsonWriter jw) throws IOException;

}
