// Copyright 2009 Google Inc. All Rights Reserved

package com.google.wave.api.oauth.impl;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

/**
 * PersistenceManagerFactory singleton wrapper class. 
 * Allows a single instance of PersistenceManagerFactory to save from costly
 * initializations.
 * 
 * @author kimwhite@google.com (Kimberly White)
 */
public final class SingletonPersistenceManagerFactory {
  private static final PersistenceManagerFactory pmfInstance =
      JDOHelper.getPersistenceManagerFactory("transactions-optional");

  /** PMF constructor. */
  private SingletonPersistenceManagerFactory() {}

  /**
   * Allows app to reuse a single instance of a PersistenceManagerFactory.
   * 
   * @return instance of persistence manager. 
   */
  public static PersistenceManagerFactory get() {
    return pmfInstance;
  }
}
