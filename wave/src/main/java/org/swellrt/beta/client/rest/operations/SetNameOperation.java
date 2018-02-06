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
 * See {@link NamingServlet}, POST operations
 *
 */
public class SetNameOperation
    extends ServerOperation<ObjectName, Void> {

  public SetNameOperation(ServiceContext context, ObjectName options,
      ServiceOperation.Callback<Void> callback) {
    super(context, options, callback, VoidImpl.class);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  public String getRestContext() {
    return "/rest/naming";
  }

  @Override
  protected void buildRestParams() throws SException {

    if (!context.hasSession()) {
      throw new SException(ResponseCode.NOT_LOGGED_IN);
    }

    if (options.getId() == null || options.getName() == null) {
      throw new SException(ResponseCode.BAD_REQUEST);
    }

    String id = options.getId();
    if (!id.contains("/")) {
      id = context.getServiceSession().getWaveDomain() + "/" + id;
    }

    addPathElement("wave");
    addPathElement(id);
    addPathElement(options.getName());

  }



}
