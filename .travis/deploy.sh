#!/bin/bash

#
# Execute this script from project base folder: 
# $ .travis/deploy.sh
#

set -e

#
# Docker
#

VERSION=`basename wave/build/libs/swellrt-*.jar | sed 's/^swellrt-//' | sed 's/.jar//'` 
mv wave/build/libs/swellrt-$VERSION.jar wave/build/libs/swellrt.jar

if [ "$TRAVIS_BRANCH" == "master" ]; then
   
   echo "Creating docker image..."
   docker build -t p2pvalue/swellrt .
   docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
   echo "Deploying docker image :latest"
   docker push p2pvalue/swellrt

elif [ -n $TRAVIS_TAG  ]; then

  if [ "$TRAVIS_TAG" == "$VERSION" ]; then

	echo "Ready to tag docker image $VERSION"

  else
	echo "Version in build.gradle doesn't match Git Tag version"
  fi
  	  	
	
fi

