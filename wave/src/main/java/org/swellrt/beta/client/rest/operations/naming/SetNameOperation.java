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
public class SetNameOperation
    extends ServerOperation<SetNameOperation.Options, SetNameOperation.Response> {


  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    /** a Wave id */
    @JsProperty
    public String getId();

    /** a Wave name */
    @JsProperty
    public String getName();
  }

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response {
  }

  public SetNameOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  public String getRestContext() {
    return "naming";
  }

  @Override
  protected void buildRestParams() throws SException {

    if (!context.isSession()) {
      throw new SException(ResponseCode.NOT_LOGGED_IN);
    }

    if (options.getId() == null || options.getName() == null) {
      throw new SException(ResponseCode.BAD_REQUEST);
    }

    String id = options.getId();
    if (!id.contains("/")) {
      id = context.getWaveDomain() + "/" + id;
    }

    addPathElement("wave");
    addPathElement(id);
    addPathElement(options.getName());

  }



}
