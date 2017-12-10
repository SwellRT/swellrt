package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.rest.ServerOperation;

import jsinterop.annotations.JsType;

/**
 * The profile data which can be retrieved from server.
 * <p>
 * Client classes must adapt this data to handy types.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(isNative = true)
public class AccountDataResponse implements ServerOperation.Response {

  public String id;
  public String email;
  public String locale;
  public String avatarUrl;
  public String sessionId;
  public String transientSessionId;
  public String domain;
  public String name;

}
