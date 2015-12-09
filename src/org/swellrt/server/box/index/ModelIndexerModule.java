/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.swellrt.server.box.index;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.swellrt.server.box.notification.DeviceStore;
import org.swellrt.server.box.notification.DeviceStoreFake;
import org.swellrt.server.box.notification.NotificationRegisterStore;
import org.swellrt.server.box.notification.NotificationRegisterStoreFake;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.authentication.SessionManagerImpl;

/**
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class ModelIndexerModule extends AbstractModule {


  public static String MONGO_COLLECTION_MODELS = "models";
  public static String MONGO_COLLECTION_MODELS_LOG = "models_log";

  @Inject
  public ModelIndexerModule() {

  }

  @Override
  public void configure() {

    bind(ModelIndexerDispatcher.class).to(ModelIndexerDispatcherImpl.class).in(Singleton.class);
    bind(SessionManager.class).to(SessionManagerImpl.class).in(Singleton.class);
    bind(NotificationRegisterStore.class).to(NotificationRegisterStoreFake.class)
        .in(Singleton.class);
    bind(DeviceStore.class).to(DeviceStoreFake.class).in(Singleton.class);
  }

}
