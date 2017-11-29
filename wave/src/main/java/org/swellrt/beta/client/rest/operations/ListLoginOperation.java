package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsType;

/**
 * Log in operation
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public final class ListLoginOperation
    extends ServerOperation<ListLoginOperation.Options, ListLoginOperation.Response> {

  public ListLoginOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }

  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

  }

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response {
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }


  @Override
  public void buildRestParams() throws SException {
    addPathElement("auth");
  }



}
