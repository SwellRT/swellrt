#!/bin/sh

EXTRA_ARGS=$*
SWELLRT_HOME=/usr/local/swellrt
SWELLRT_JAR=${SWELLRT_HOME}/wave.jar

java $DEBUG_FLAGS \
  -Djava.util.logging.config.file=wiab-logging.conf \
  -Djava.security.auth.login.config=jaas.config \
  -Dwave.server.config=server.config \
  -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog \
  -Dorg.eclipse.jetty.level=DEBUG \
  $EXTRA_ARGS \
  -jar $SWELLRT_JAR
