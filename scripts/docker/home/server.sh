#!/bin/sh

EXTRA_ARGS=$*
SWELLRT_HOME=/usr/local/swellrt
SWELLRT_JAR=${SWELLRT_HOME}/swellrt.jar

if [ ! -z $MONGODB_HOST ]; then EXTRA_ARGS=${EXTRA_ARGS}" -Dcore.mongodb_host="${MONGODB_HOST}; fi
if [ ! -z $MONGODB_PORT ]; then	EXTRA_ARGS=${EXTRA_ARGS}" -Dcore.mongodb_port="${MONGODB_PORT}; fi
if [ ! -z $MONGODB_DB ]; then EXTRA_ARGS=${EXTRA_ARGS}" -Dcore.mongodb_database="${MONGODB_DB}; fi
if [ ! -z $SWELLRT_PARAMS ]; then  EXTRA_ARGS=${EXTRA_ARGS}" "${SWELLRT_PARAMS}; fi

echo "Starting SwellRT ["$EXTRA_ARGS"]"

java $DEBUG_FLAGS \
  -Djava.util.logging.config.file=${SWELLRT_HOME}/config/swellrt-logging.conf \
  -Djava.security.auth.login.config=${SWELLRT_HOME}/config/jaas.config \
  -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog \
  -Dorg.eclipse.jetty.level=DEBUG \
  $EXTRA_ARGS \
  -jar $SWELLRT_JAR
