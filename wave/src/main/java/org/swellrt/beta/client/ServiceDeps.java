package org.swellrt.beta.client;

import org.swellrt.beta.client.rest.ServerOperationExecutor;

/**
 * Platform-dependent global dependencies regarding Swell client stuff.
 */
public class ServiceDeps {

  /** Factory of service's sessions */
  public static ServiceSession.Factory serviceSessionFactory = null;

  /** Executor of operations provided by the Swell server */
  public static ServerOperationExecutor remoteOperationExecutor = null;

  /** The runtime service context */
  public static ServiceContext serviceContext = null;

}
