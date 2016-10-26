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

package org.waveprotocol.wave.client.account.impl;


import java.util.List;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.common.base.Joiner;

/**
 * A {@link Profile} which determines all properties from just a
 * {@link ParticipantId} given on construction.
 *
 * @author kalman@google.com (Benjamin Kalman)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public final class ProfileImpl implements Profile {
  
  private final static String ANONYMOUS_NAME = "Anonymous";

  private final ParticipantId id;
  
  private String name;
  private String imageUrl;  
  private String shortName;


  public ProfileImpl(ParticipantId id, String name, String imageUrl) {
    this.id = id;
    this.shortName = buildName(id);
    this.name = (name == null || name.isEmpty()) ? shortName : name;
    this.imageUrl = imageUrl;
  }

  
  @Override
  public ParticipantId getParticipantId() {
    return id;
  }

  @Override
  public String getAddress() {
    return id.getAddress();
  }

  
  @Override
  public String getImageUrl() {
    if (imageUrl == null) {
      return "static/images/unknown.jpg";
    }
    return imageUrl;
  }

  private static String buildName(ParticipantId id) {
    List<String> names = CollectionUtils.newArrayList();
    String nameWithoutDomain = id.getName();
    
    if (ParticipantId.isAnonymousName(nameWithoutDomain)) {
      return ANONYMOUS_NAME;      
    } else if (nameWithoutDomain != null && !nameWithoutDomain.isEmpty()) {
      // Include empty names from fragment, so split with a -ve.
      for (String fragment : nameWithoutDomain.split("[._]", -1)) {
        if (!fragment.isEmpty()) {
          names.add(capitalize(fragment));
        }
      }
      // ParticipantId normalization implies names can not be empty.
      assert !names.isEmpty();      
      return Joiner.on(' ').join(names);
    } else {
      return "";
    }
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : (Character.toUpperCase(s.charAt(0))) + s.substring(1);
  }

  @Override
  public String toString() {
    return "ProfileImpl [id=" + id + ", "+name+ ", imageUrl=" + imageUrl + "]";
  }

  @Override
  public String getName() {    
    return name;
  }

  @Override
  public String getShortName() {
    if (shortName == null)
      shortName = capitalize(id.getName());
    
    return shortName;
  }

  
  public void update(String name, String imageUrl) {
    this.name = (name == null || name.isEmpty()) ? shortName : name;
    this.imageUrl = imageUrl;
  }


}
