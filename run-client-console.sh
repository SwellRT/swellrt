#!/bin/bash

# This script will start the Wave in a Box wave client.
#

# Function to extract a property from a file
extract_property () {
  if [ ! -e $1 ]; then
    echo "Property file '$1' does not exist!"
    exit 1;
  fi
  echo `sed "s/[\\t ]*=[\\t ]*/=/g" $1 | grep ^$2= | cut -f2 -d=`
}

# Make sure the config file exists.
if [ ! -e server.config ]; then
  echo "You need to copy server.config.example to server.config and edit it."
  exit 1
fi

# Domain name of the wave server 
WAVE_SERVER_DOMAIN_NAME=$(extract_property server.config wave_server_domain)

# Public address of server http frontend
HTTP_SERVER_PUBLIC_ADDRESS=$(extract_property server.config http_frontend_public_address)

# The version of Wave in a Box, extracted from the build.properties file
WAVEINABOX_VERSION=$(extract_property build.properties waveinabox.version)

. process-script-args.sh

if [[ $ARGC != 1 ]]; then
  echo "usage: ${0} <username EXCLUDING DOMAIN>"
else
  USER_NAME=${ARGV[0]}@$WAVE_SERVER_DOMAIN_NAME
  echo "running client as user: ${USER_NAME}"
  exec java $DEBUG_FLAGS -jar dist/waveinabox-client-console-$WAVEINABOX_VERSION.jar $USER_NAME $HTTP_SERVER_PUBLIC_ADDRESS
fi
