SWELLRT_HOME=/usr/local/swellrt
SWELLRT_CONSOLE_LOGFILE=${SWELLRT_HOME}/console.log
SWELLRT_JAR=${SWELLRT_HOME}/wave.jar

nohup java $DEBUG_FLAGS \
  -Djava.util.logging.config.file=wiab-logging.conf \
  -Djava.security.auth.login.config=jaas.config \
  -Dwave.server.config=server.config \
  -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog \
  -Dorg.eclipse.jetty.level=DEBUG \
  -jar $SWELLRT_JAR >> $SWELLRT_CONSOLE_LOGFILE 2>> $SWELLRT_CONSOLE_LOGFILE  &
