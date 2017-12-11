package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.AccountImpl;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsType;

public final class GetUserOperation
    extends ServerOperation<Credential, Account> {


  @JsType(isNative = true)
  public interface Response extends Account, ServerOperation.Response {

  }


  public GetUserOperation(ServiceContext context, Credential options,
      ServiceOperation.Callback<Account> callback) {
    super(context, options, callback, AccountImpl.class);
  }



  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }


  @Override
  protected void buildRestParams() throws SException {

    if (!context.isSession()) {
      throw new SException(SException.NOT_LOGGED_IN);
    }

    addPathElement("account");
    addPathElement(context.getParticipantId());
  }



}
