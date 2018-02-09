package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.ObjectName;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.client.rest.operations.params.VoidImpl;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;

/**
 * See {@link NamingServlet}, GET operations
 *
 */
public class GetDocVersionLog
    extends ServerOperation<ObjectName, Void> {




  public GetDocVersionLog(ServiceContext context, ObjectName options,
      ServiceOperation.Callback<Void> callback) {
    super(context, options, callback, VoidImpl.class);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }

  @Override
  public String getRestContext() {
    return "/rest/data";
  }


  @Override
  protected void buildRestParams() throws SException {

    if (options.getId() != null) {
      addPathElement("wave");

      String id = options.getId();
      if (!id.contains("/")) {
        id = context.getServiceSession().getWaveDomain() + "/" + id;
      }

      addPathElement(id);

    } else if (options.getName() != null) {
      addPathElement("name");
      addPathElement(options.getName());

    } else {
      throw new SException(ResponseCode.BAD_REQUEST);
    }
  }


}
