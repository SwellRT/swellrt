package org.swellrt.beta.client.rest.operations.naming;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.box.server.swell.NamingServlet;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * See {@link NamingServlet}, POST operations
 *
 */
public class DeleteNameOperation
    extends ServerOperation<DeleteNameOperation.Options, DeleteNameOperation.Response> {


  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    /** a Wave id (required) */
    @JsProperty
    public String getId();

    /** a Wave name (optional) */
    @JsProperty
    public String getName();
  }

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response {
  }

  public DeleteNameOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);

  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.DELETE;
  }

  @Override
  public void buildRestParams() throws SException {

    if (!getContext().isSession()) {
      doFailure(new SException(ResponseCode.NOT_LOGGED_IN));
      return;
    }

    if (getOptions().getId() == null) {
      doFailure(new SException(ResponseCode.BAD_REQUEST));
      return;
    }

    String id = getOptions().getId();
    if (!id.contains("/")) {
      id = getContext().getWaveDomain() + "/" + id;
    }

    addPathElement("wave");
    addPathElement(id);

    if (getOptions().getName() != null)
      addPathElement(getOptions().getName());

  }

  @Override
  public String getRestContext() {
    return "naming";
  }
}
