package org.swellrt.beta.client.rest;

public interface JsonParser {

  <R, S extends R> R parse(String json, Class<S> implType);

}
