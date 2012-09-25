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

# This script will start the Wave in a Box server.

# Make sure the config file exists.
if [ ! -e server.config ]; then
  echo "You need to copy server.config.example to server.config and edit it. Or run: 'ant -f server-config.xml' to generate the file automatically."
  exit 1
fi

# The version of Wave in a Box, extracted from the build.properties file
WAVEINABOX_VERSION=`sed "s/[\\t ]*=[\\t ]*/=/g" build.properties | grep ^waveinabox.version= | cut -f2 -d=`

. process-script-args.sh

exec java $DEBUG_FLAGS \
  -Dorg.eclipse.jetty.LEVEL=DEBUG \
  -Djava.security.auth.login.config=jaas.config \
  -Dwave.server.config=server.config \
  -jar dist/waveinabox-server-$WAVEINABOX_VERSION.jar
