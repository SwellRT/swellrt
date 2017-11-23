package org.swellrt.beta.client.operation.impl.naming;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.box.server.swell.NamingServlet;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * See {@link NamingServlet}, POST operations
 *
 */
public class SetNameOperation
    extends HTTPOperation<SetNameOperation.Options, SetNameOperation.Response> {

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


  public SetNameOperation(ServiceContext context) {
    super(context);
  }

  @Override
  public void execute(Options options, Callback<Response> callback) {

    if (!getServiceContext().isSession()) {
      if (callback != null)
        callback.onError(new SException(ResponseCode.NOT_LOGGED_IN));
    }

    if (options.getId() == null || options.getName() == null) {
      if (callback != null)
        callback.onError(new SException(ResponseCode.BAD_REQUEST));
    }

    String id = options.getId();
    if (!id.contains("/")) {
      id = getServiceContext().getWaveDomain() + "/" + id;
    }

    setPathContext("naming");
    addPathElement("wave");
    addPathElement(id);
    addPathElement(options.getName());


    executePost(callback);
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
