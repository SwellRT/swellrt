#!/bin/bash
VERSION=0.4-SNAPSHOT

if [[ $# -gt 0 ]]
then
  ARTIFACTS=$*
else
  ARTIFACTS="compact-client compact-client-src proto proto-src proto-msg"
fi

ant dist-proto dist-pst dist-pst-dep dist-pst dist-compact-client dist-compact-client-src

for i in $ARTIFACTS
do
  cd dist
  mvn install:install-file -DgroupId=org.waveprotocol -DartifactId=$i -Dversion=$VERSION -Dfile=$i.jar  -Dpackaging=jar
  cd ..
done


