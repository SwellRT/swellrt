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
cp wave/build/libs/swellrt-$VERSION.jar wave/build/libs/swellrt.jar

if [ "$TRAVIS_BRANCH" == "master" ]; then
   echo "Creating docker image..."
   docker build -t p2pvalue/swellrt .
   docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
   echo "Deploying docker image :latest"
   docker push p2pvalue/swellrt
fi

if [ -n "$TRAVIS_TAG" ]; then
    echo "Travis tag is set to: $TRAVIS_TAG"
    echo "SwellRT version at wave/build.gradle is set to: $VERSION"
fi

if [[ "$TRAVIS_TAG" == *-alpha && "v$VERSION" == "$TRAVIS_TAG"]]; then
   echo "Creating docker image..."
   docker build -t p2pvalue/swellrt:"$VERSION" .
   docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
   echo "Deploying docker image: p2pvalue/swellrt:$VERSION"
   docker push p2pvalue/swellrt:"$VERSION"
fi

#
# Reload services
#
#if [ "$TRAVIS_BRANCH" == "master" ]; then
#   echo "Preparing to update services..."
#   openssl aes-256-cbc -K $encrypted_dd5995a51c0a_key -iv $encrypted_dd5995a51c0a_iv -in .travis/secrets.tar.enc -out .travis/secrets.tar -d
#   tar xvf .travis/secrets.tar --directory .travis
#   chmod ugo+x .travis/update_services.sh
#   .travis/update_services.sh
#fi
