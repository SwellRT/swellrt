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

package org.waveprotocol.box.server.account;

import org.waveprotocol.box.server.authentication.PasswordDigest;

/**
 * {@link HumanAccountData} representing an account from a human.
 *
 *  Stores the user's authentication information.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author josephg@gmail.com (Joseph Gentle)
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public interface HumanAccountData extends AccountData {
  /**
   * Gets the user's password digest. The digest can be used to authenticate the
   * user.
   *
   *  This method will return null if password based authentication is disabled
   * for the user, or if no password is set.
   *
   * @return The user's password digest, or null if password authentication is
   *         disabled for the user, or no password is set.
   */
  PasswordDigest getPasswordDigest();

  /**
   * Enable password changes for the account.
   *
   * @param digest
   */
  void setPasswordDigest(PasswordDigest digest);

  /**
   * Gets user's locale.
   *
   * @return The user's locale.
   */
  String getLocale();

  /**
   * Sets the user's locale.
   *
   */
  void setLocale(String locale);

  /**
   * Sets the user's email.
   *
   * @param email
   * @throws InvalidEmailException
   */
  void setEmail(String email);

  /**
   * Get the user's email
   *
   * @return the user's email or null if not set.
   */
  String getEmail();

  /**
   * Sets the recovery secret token for password restore
   *
   * @param token
   */
  void setRecoveryToken(String token);

  /**
   * Sets the recovery secret token for password restore
   *
   * @param token
   */
  void setRecoveryToken(SecretToken token);


  /**
   * Gets the recovery secret token for password restore
   *
   * @return
   */
  SecretToken getRecoveryToken();


  /**
   * Sets the name of the avatar's image file including its mime type:
   * 
   * image/png;02DE23425235SDFED2341A.png
   * 
   * @param the mime type and the file name separated by a ;
   */
  void setAvatarFileId(String fileName);


  /**
   * Gets the name of the avatar's image file
   * 
   */
  String getAvatarFileName();


  /**
   * Gets the avatar's file mime type
   * 
   * @return
   */
  String getAvatarMimeType();

  /**
   * 
   * @return
   */
  String getAvatarFileId();
  
  /**
   * Get the participant name
   * @return
   */
  String getName();
  
  /**
   * Set the participant name
   * @return
   */
  void setName(String name);


}
