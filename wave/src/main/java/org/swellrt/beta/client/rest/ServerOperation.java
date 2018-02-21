package org.swellrt.beta.client.rest;

import java.util.Collections;
import java.util.Map;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.common.SException;

public abstract class ServerOperation<O extends ServiceOperation.Options, R extends ServiceOperation.Response>
    extends ServiceOperation<O, R>
{

  public enum Method {
    GET, POST, DELETE
  };

  //
  // ServerOperation state
  //

  private static final String DEFAULT_REQUEST_CONTEXT = "swell";

  private final static String pathSeparator = "/";
  private final static String querySeparator = "?";
  private final static String queryAmp = "&";
  private final static String queryEquals = "=";

  private final Class<? extends R> responseImplClass;

  private String path = "";
  private String query = "";

  public ServerOperation(ServiceContext context, O options, Callback<R> callback,  Class<? extends R> responseImplClass) {
    super(context, options, callback);
    this.responseImplClass = responseImplClass;
  }

  public ServerOperation(ServiceContext context, O options, Callback<R> callback) {
    super(context, options, callback);
    this.responseImplClass = null;
  }

  protected ServerOperation<O, R> addPathElement(String element) {
    path += pathSeparator + element;
    return this;
  }

  protected ServerOperation<O, R> addQueryParam(String param, String value) {
    if (query.isEmpty())
      query += querySeparator;
    else
      query += queryAmp;

    query += param + queryEquals + value;
    return this;

  }


  public String getRestContext() {
    return DEFAULT_REQUEST_CONTEXT;
  }

  public final String getRestParams() throws SException {
    path = "";
    query = "";
    buildRestParams();

    return path + query;
  }

  public abstract Method getMethod();


  protected abstract void buildRestParams() throws SException;

  protected Map<String, String> getHeaders() {
    return Collections.emptyMap();
  }

  /** By default parse JSON. Overwrite to process response in other formats */
  protected void doSuccessRaw(String raw) {
    doSuccess(WaveDeps.json.parse(raw, responseImplClass));
  }

  public boolean sendSessionInUrl() {
    return true;
  }

  public boolean sendOptionsAsBody() {
    return false;
  }



}
