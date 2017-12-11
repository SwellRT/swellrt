package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;

import jsinterop.annotations.JsType;

public final class QueryOperation
    extends ServerOperation<QueryOperation.Options, QueryOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    public String getQuery();
    public String getProjection();
    public String getAggregate();

  }

  @JsType
  public interface Response extends ServerOperation.Response {


  }


  public QueryOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback, new Response() {
    }.getClass());
  }

  @Override
  public ServerOperation.Method getMethod() {
    return null;
  }

  @Override
  protected void buildRestParams() {

  }


}
