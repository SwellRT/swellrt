package org.swellrt.beta.client.rest;

public abstract class OperationExecutor<O extends ServiceOperation.Options, R extends ServiceOperation.Response> {

  public abstract void execute(ServiceOperation<O, R> operation);

}
