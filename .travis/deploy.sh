#!/bin/bash

#
# Execute this script from project base folder: 
# $ .travis/deploy.sh
#

set -e

#
# Docker
#

DOCKER_IMAGE="p2pvalue/swellrt"
DOCKER_TAG=""
VERSION=`basename wave/build/libs/swellrt-*.jar | sed 's/^swellrt-//' | sed 's/.jar//'` 

mv wave/build/libs/swellrt-$VERSION.jar wave/build/libs/swellrt.jar

if [ -n $TRAVIS_TAG ]; then

  if [ "$TRAVIS_TAG" == "$VERSION"]; then
	echo "A Git tag has been pushed. Ready to tag docker image as "$VERSION
	DOCKER_TAG="-t "$DOCKER_IMAGE":"$VERSION
  else
	echo "A Git tag has been pushed, but it doesn't match generated Jar version"
	echo "Did you tag the right commit? Did you forget to update build.gradle?"
	echo ""
	echo "Build aborted!!!" 
	
	exit -1
  fi	
fi



if [ "$TRAVIS_BRANCH" == "master" || -n $TRAVIS_TAG ]; then
   
   echo "Creating Docker image..."
   docker build -t $DOCKER_IMAGE":latest" $DOCKER_TAG .
   docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
   echo "Deploying Docker image"
   docker push $DOCKER_IMAGE 

fi
