#!/bin/bash

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# This script will test your certificates, verifying that
# the options are set correctly in run-config.sh, that the
# public and private keys match, and that the whole certificate
# chain can be verified up to the root certificate.

if [ -r run-config.sh ]; then
  . run-config.sh
else
  echo "You need to copy run-config.sh.example to run-config.sh and configure"; exit 1
fi

if [ $WAVESERVER_DISABLE_VERIFICATION != "false" ]; then
  echo "ERROR: WAVESERVER_DISABLE_VERIFICATION should be set to false" 
  exit 1
fi

if [ $WAVESERVER_DISABLE_SIGNER_VERIFICATION != "false" ]; then
  echo "ERROR: WAVESERVER_DISABLE_SIGNER_VERIFICATION should be set to false" 
  exit 1
fi

if [ ! -e $PRIVATE_KEY_FILENAME ]; then
  echo "ERROR: Private key does not exist:" $PRIVATE_KEY_FILENAME
  exit 1
fi

# Break apart the certificate list on the commas.
certlist=(`echo $CERTIFICATE_FILENAME_LIST | sed 's/,/ /g'`) 

if [ "`openssl x509 -modulus -in ${certlist[0]} -noout`" != "`openssl \
  rsa -in $PRIVATE_KEY_FILENAME  -modulus -noout`" ]; then
  echo "ERROR: Public and private key do not match!"
  exit 1
fi

# Reverse the order of the list for passing into openssl.
len=${#certlist[@]} 
for (( i = 0; $i < $len/2; i++ )); do 
  swap=$len-$i-1
  tmp=${certlist[i]}
  certlist[i]=${certlist[$swap]} 
  certlist[$swap]=$tmp 
done 

# Verify that each file in the certificate list exists.
for (( i=0; $i < $len; i++ )); do 
  if [ ! -e ${certlist[$i]} ]; then
    echo "ERROR: Certificate file does not exist:" ${certlist[$i]}
    exit 1
  fi
done

# Verify the certificate chain.
if (( $len > 1 )); then
  verifycmd="openssl verify -CAfile ${certlist[@]}"
else
  verifycmd="openssl verify ${certlist[@]}" 
fi

if $verifycmd | grep -q "OK$" ; then 
  echo "SUCCESS: The certificates have been verified and are working correctly"
else
  echo "ERROR: Certificate chain failed to verify"
  $verifycmd 
fi
