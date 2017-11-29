package org.swellrt.beta.client.rest.operations.naming;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * See {@link NamingServlet}, GET operations
 *
 */
public class GetNamesOperation
    extends ServerOperation<GetNamesOperation.Options, GetNamesOperation.Response> {


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

  public GetNamesOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }

  @Override
  public String getRestContext() {
    return "naming";
  }


  @Override
  public void buildRestParams() throws SException {

    if (getOptions().getId() != null) {
      addPathElement("wave");

      String id = getOptions().getId();
      if (!id.contains("/")) {
        id = getContext().getWaveDomain() + "/" + id;
      }

      addPathElement(id);

    } else if (getOptions().getName() != null) {
      addPathElement("name");
      addPathElement(getOptions().getName());

    }
  }


}
