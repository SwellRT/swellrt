package org.swellrt.beta.client.rest;

public abstract class OperationExecutor {

  public abstract void execute(
      ServiceOperation<? extends ServiceOperation.Options, ? extends ServiceOperation.Response> operation);

}
