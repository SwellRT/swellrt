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

package org.waveprotocol.wave.util.settings;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A property file parsing system that converts a given
 * settings class into a Guice module with injectable
 * @Named parameters.
 *
 * Originally based on some CLI work by arb@google.com (Anthony Baxter).
 * Refactored by tad.glines@gmail.com (Tad Glines) to use Commons Configuration and add support for
 * List<String>.
 */
public class SettingsBinder {

  /**
   * Used to validate that a type is supported. Some types may have generic parameters that need
   * to be checked.
   */
  private interface SettingTypeValidator {
    boolean check(Type type);
  }

  private static final Map<Type, SettingTypeValidator> supportedSettingTypes;

  /**
   * This default validator just returns true.
   */
  private static final SettingTypeValidator DEFAULT_TYPE_VALIDATOR = new SettingTypeValidator() {
    @Override
    public boolean check(Type type) {
      return true;
    }
  };

  /**
   * This validator checks to make sure the {@link List}'s generic parameter is also supported.
   */
  private static final SettingTypeValidator LIST_TYPE_VALIDATOR = new SettingTypeValidator() {
    @Override
    public boolean check(Type type) {
      if (type instanceof ParameterizedType) {
        Type[] args = ((ParameterizedType)type).getActualTypeArguments();
        if (args.length == 1) {
          // At the moment only List<String> is supported.
          if (args[0] == String.class) {
            return true;
          }
        }
      }
      return false;
    }
  };

  static {
    ImmutableMap.Builder<Type, SettingTypeValidator> builder = ImmutableMap.builder();
    builder.put(int.class, DEFAULT_TYPE_VALIDATOR);
    builder.put(boolean.class, DEFAULT_TYPE_VALIDATOR);
    builder.put(String.class, DEFAULT_TYPE_VALIDATOR);
    builder.put(List.class, LIST_TYPE_VALIDATOR);
    supportedSettingTypes = builder.build();
  }

  /**
   * Bind configuration parameters into Guice Module.
   *
   * @return a Guice module configured with setting support.
   * @throws ConfigurationException on configuration error
   */
  public static Module bindSettings(String propertiesFileKey, Class<?>... settingsArg)
      throws ConfigurationException {
    final CompositeConfiguration config = new CompositeConfiguration();
    config.addConfiguration(new SystemConfiguration());
    String propertyFile = config.getString(propertiesFileKey);
    if (propertyFile != null) {
      config.addConfiguration(new PropertiesConfiguration(propertyFile));
    }

    List<Field> fields = new ArrayList<Field>();
    for (Class<?> settings : settingsArg) {
      fields.addAll(Arrays.asList(settings.getDeclaredFields()));
    }

    // Reflect on settings class and absorb settings
    final Map<Setting, Field> settings = new LinkedHashMap<Setting, Field>();
    for (Field field : fields) {
      if (!field.isAnnotationPresent(Setting.class)) {
        continue;
      }

      // Validate target type
      SettingTypeValidator typeHelper = supportedSettingTypes.get(field.getType());
      if (typeHelper == null || !typeHelper.check(field.getGenericType())) {
        throw new IllegalArgumentException(field.getType()
            + " is not one of the supported setting types");
      }

      Setting setting = field.getAnnotation(Setting.class);
      settings.put(setting, field);
    }

    // Now validate them
    List<String> missingProperties = new ArrayList<String>();
    for (Setting setting : settings.keySet()) {
      if (setting.defaultValue().isEmpty()) {
        if (!config.containsKey(setting.name())) {
          missingProperties.add(setting.name());
        }
      }
    }
    if (missingProperties.size() > 0) {
      StringBuilder error = new StringBuilder();
      error.append("The following required properties are missing from the server configuration: ");
      error.append(Joiner.on(", ").join(missingProperties));
      throw new ConfigurationException(error.toString());
    }

    // bundle everything up in an injectable guice module
    return new AbstractModule() {

      @Override
      protected void configure() {
        // We must iterate the settings a third time when binding.
        // Note: do not collapse these loops as that will damage
        // early error detection. The runtime is still O(n) in setting count.
        for (Map.Entry<Setting, Field> entry : settings.entrySet()) {
          Class<?> type = entry.getValue().getType();
          Setting setting = entry.getKey();

          if (int.class.equals(type)) {
            Integer defaultValue = null;
            if (!setting.defaultValue().isEmpty()) {
              defaultValue = Integer.parseInt(setting.defaultValue());
            }
            bindConstant().annotatedWith(Names.named(setting.name()))
                .to(config.getInteger(setting.name(), defaultValue));
          } else if (boolean.class.equals(type)) {
            Boolean defaultValue = null;
            if (!setting.defaultValue().isEmpty()) {
              defaultValue = Boolean.parseBoolean(setting.defaultValue());
            }
            bindConstant().annotatedWith(Names.named(setting.name()))
                .to(config.getBoolean(setting.name(), defaultValue));
          } else if (String.class.equals(type)) {
            bindConstant().annotatedWith(Names.named(setting.name()))
                .to(config.getString(setting.name(), setting.defaultValue()));
          } else {
            String[] value = config.getStringArray(setting.name());
            if (value.length == 0 && !setting.defaultValue().isEmpty()) {
              value = setting.defaultValue().split(",");
            }
            bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named(setting.name()))
                .toInstance(ImmutableList.copyOf(value));
          }
        }
      }
    };
  }
}
