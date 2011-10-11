#!/bin/sh

#
# This script is a wrappeer around run-seerver.sh that redirects output to a log file.
# It is used by daemon-script.sh
#

# Exit if WAVE_HOME isn't defined or points to a non-existant directory.
if [ -z "$WAVE_HOME" -o ! -d "$WAVE_HOME" ]; then
    echo WAVE_HOME is not defined!
    exit 1
fi

# Exit if there is no log dir under $WAVE_HOME
if [ ! -d $WAVE_HOME/log ]; then
    echo "The log dir ${WAVE_HOME}/log doesn't exist"
    exit 1
fi

cd $WAVE_HOME

DATE=`date +%Y%m%d_%H%M%S`

# Create a symbolic link named wave.log that will point to the msot recent log file.
rm -f ${WAVE_HOME}/log/wave.log
ln -s ${WAVE_HOME}/log/wave_${DATE}.log ${WAVE_HOME}/log/wave.log

# Exec the wave server so that the daemon script can track the pid.
exec ${WAVE_HOME}/run-server.sh > ${WAVE_HOME}/log/wave_${DATE}.log 2>&1
