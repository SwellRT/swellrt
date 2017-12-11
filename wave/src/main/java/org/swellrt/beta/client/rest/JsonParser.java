package org.swellrt.beta.client.rest;

public interface JsonParser {

  <T, R extends T> T parse(String json, Class<T> interfaceType, Class<R> dataType);

}
