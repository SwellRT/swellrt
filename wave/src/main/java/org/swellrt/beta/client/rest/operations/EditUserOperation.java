package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.AccountImpl;
import org.swellrt.beta.common.SException;

public final class EditUserOperation
    extends ServerOperation<Account, Account> {


  public EditUserOperation(ServiceContext context, Account options,
      ServiceOperation.Callback<Account> callback) {
    super(context, options, callback, AccountImpl.class);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  public boolean sendOptionsAsBody() {
    return true;
  }

  @Override
  protected void buildRestParams() throws SException {

    if (!context.hasSession()) {
      throw new SException(SException.NOT_LOGGED_IN);
    }

    addPathElement("account");
    addPathElement(context.getServiceSession().getParticipantId().getAddress());
  }

  @Override
  protected void doSuccess(Account response) {
    context.update(response);
    super.doSuccess(response);
  }

}
