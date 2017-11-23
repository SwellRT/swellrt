package org.swellrt.beta.client.operation.impl.naming;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.box.server.swell.NamingServlet;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * See {@link NamingServlet}, GET operations
 *
 */
public class GetNamesOperation
    extends HTTPOperation<GetNamesOperation.Options, GetNamesOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    /** a Wave id */
    @JsProperty
    public String getId();

    /** a Wave name */
    @JsProperty
    public String getName();
  }

  @JsType(isNative = true)
  public interface Response extends Operation.Response {
    // TODO map JSON response to Java
  }


  public GetNamesOperation(ServiceContext context) {
    super(context);
  }

  @Override
  public void execute(Options options, Callback<Response> callback) {
    setPathContext("naming");

    if (options.getId() != null) {
      addPathElement("wave");

      String id = options.getId();
      if (!id.contains("/")) {
        id = getServiceContext().getWaveDomain() + "/" + id;
      }

      addPathElement(id);

    } else if (options.getName() != null) {
      addPathElement("name");
      addPathElement(options.getName());

    }

    executeGet(callback);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null) {
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
    }
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {
    if (callback != null) {
      callback.onSuccess(generateResponse(data));
    }
  }

}
