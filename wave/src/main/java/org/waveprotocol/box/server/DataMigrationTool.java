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

package org.waveprotocol.box.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import org.waveprotocol.box.server.persistence.PersistenceModule;
import org.waveprotocol.box.server.persistence.migration.DeltaMigrator;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.wave.util.settings.Setting;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * A cmd line utility to perform data migration from a store type to another
 * one. Initially developed to replicate deltas from a file store to a mongodb
 * store.
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class DataMigrationTool {

  private static final Log LOG = Log.get(DataMigrationTool.class);



  private static void runDeltasMigration(Injector sourceInjector, Injector targetInjector) {

    // We can migrate data from-to any store type,
    // but it is not allowed migrate from-to the same type
    String sourceDeltaStoreType =
        sourceInjector
            .getInstance(Key.get(String.class, Names.named(CoreSettings.DELTA_STORE_TYPE)));

    String targetDeltaStoreType =
        targetInjector
            .getInstance(Key.get(String.class, Names.named(CoreSettings.DELTA_STORE_TYPE)));

    if (sourceDeltaStoreType.equalsIgnoreCase(targetDeltaStoreType))
      usageError("Source and Target Delta store types must be different");


    DeltaMigrator dm =
        new DeltaMigrator(sourceInjector.getInstance(DeltaStore.class),
            targetInjector.getInstance(DeltaStore.class));

    dm.run();

  }

  private static Map<Setting, Field> getCoreSettings() {

    // Get all method fields
    Field[] coreSettingFields = CoreSettings.class.getDeclaredFields();

    // Filter only annotated fields
    Map<Setting, Field> settings = new HashMap<Setting, Field>();

    for (Field f : coreSettingFields) {
      if (f.isAnnotationPresent(Setting.class)) {
        Setting setting = f.getAnnotation(Setting.class);
        settings.put(setting, f);
      }
    }

    return settings;

  }

  private static Module bindCmdLineSettings(String cmdLineProperties) {

    // Get settings from cmd line, e.g.
    // Key = delta_store_type
    // Value = mongodb
    final Map<String, String> propertyMap = new HashMap<String, String>();

    for (String arg : cmdLineProperties.split(",")) {
      String[] argTokens = arg.split("=");
      propertyMap.put(argTokens[0], argTokens[1]);
    }

    // Validate settings against CoreSettings
    final Map<Setting, Field> coreSettings = getCoreSettings();

    // Set a suitable map to match cmd line settings
    final Map<String, Setting> propertyToSettingMap = new HashMap<String, Setting>();
    for (Setting s : coreSettings.keySet()) {
      propertyToSettingMap.put(s.name(), s);
    }

    for (String propertyKey : propertyMap.keySet()) {
      if (!propertyToSettingMap.containsKey(propertyKey))
        usageError("Wrong setting '" + propertyKey + "'");
    }



    return new AbstractModule() {

      @Override
      protected void configure() {

        // We must iterate the settings when binding.
        // Note: do not collapse these loops as that will damage
        // early error detection. The runtime is still O(n) in setting count.
        for (Map.Entry<Setting, Field> entry : coreSettings.entrySet()) {

          Setting setting = entry.getKey();
          Class<?> type = entry.getValue().getType();
          String value =
              propertyMap.containsKey(setting.name()) ? propertyMap.get(setting.name()) : setting
                  .defaultValue();
          if (int.class.equals(type)) {
            // Integer defaultValue = null;
            // if (!setting.defaultValue().isEmpty()) {
            // defaultValue = Integer.parseInt(setting.defaultValue());
            // }
            bindConstant().annotatedWith(Names.named(setting.name())).to(Integer.parseInt(value));
          } else if (boolean.class.equals(type)) {
            // Boolean defaultValue = null;
            // if (!setting.defaultValue().isEmpty()) {
            // defaultValue = Boolean.parseBoolean(setting.defaultValue());
            // }
            bindConstant().annotatedWith(Names.named(setting.name())).to(
                Boolean.parseBoolean(value));
          } else if (String.class.equals(type)) {
            bindConstant().annotatedWith(Names.named(setting.name())).to(value);
          } else {
            /** Not supported **/
            /*
             * String[] value = config.getStringArray(setting.name()); if
             * (value.length == 0 && !setting.defaultValue().isEmpty()) { value
             * = setting.defaultValue().split(","); } bind(new
             * TypeLiteral<List<String>>()
             * {}).annotatedWith(Names.named(setting.name()))
             * .toInstance(ImmutableList.copyOf(value));
             */
          }
        }
      }


    };

  }

  public static void usageError() {
    usageError("");
  }

  public static void usageError(String msg) {
    System.out.println(msg + "\n");
    System.out.println("Use: DataMigrationTool <data type> <source options> <target options>\n");
    System.out.println("supported data types : deltas");
    System.out
        .println("source options example : delta_store_type=file,delta_store_directory=./_deltas");
    System.out
        .println("target options example : delta_store_type=mongodb,mongodb_host=127.0.0.1,mongodb_port=27017,mongodb_database=wiab");
    System.exit(1);
  }

  public static void main(String... args) {

    if (args.length != 3) usageError();

    String dataType = args[0];

    Module sourceSettings = bindCmdLineSettings(args[1]);
    Injector sourceSettingsInjector = Guice.createInjector(sourceSettings);
    Module sourcePersistenceModule = sourceSettingsInjector.getInstance(PersistenceModule.class);
    Injector sourceInjector = sourceSettingsInjector.createChildInjector(sourcePersistenceModule);


    Module targetSettings = bindCmdLineSettings(args[2]);
    Injector targetSettingsInjector = Guice.createInjector(targetSettings);
    Module targetPersistenceModule = targetSettingsInjector.getInstance(PersistenceModule.class);
    Injector targetInjector = targetSettingsInjector.createChildInjector(targetPersistenceModule);


    if (dataType.equals("deltas")) {

      runDeltasMigration(sourceInjector, targetInjector);


    } else {
      usageError("Wrong data type");
    }


  }

}
