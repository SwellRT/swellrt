#!/bin/bash

# This script will start the Wave Import.
#

# The version of Wave in a Box, extracted from the build.properties file
WAVEINABOX_VERSION=`sed "s/[\\t ]*=[\\t ]*/=/g" build.properties | grep ^waveinabox.version= | cut -f2 -d=`

exec java -cp dist/waveinabox-import-$WAVEINABOX_VERSION.jar org.waveprotocol.box.waveimport.WaveImport $*
