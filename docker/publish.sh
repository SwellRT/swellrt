#!/bin/sh

# The name of the docker image
NAME=p2pvalue/swellrt

# Should we push from remote server?
PUSH_SERVER=$1
echo ".${PUSH_SERVER}."

# Should we push the tag?
PUSH_TAG=0

push_tag () {
  if [ -z $PUSH_SERVER ]
  then
    docker push $1

    if [ $? -ne 0 ]; then exit 1; fi
  else
    docker save -o /tmp/swellrt.docker $1
    scp /tmp/swellrt.docker $PUSH_SERVER:/tmp
    ssh $PUSH_SERVER "docker load -i /tmp/swellrt.docker; docker push $1"

    if [ $? -ne 0 ]; then exit 1; fi
  fi
}

echo "Building image.."

docker build -t $NAME .

if [ $? -ne 0 ]; then exit 1; fi

echo "Tagging image.."

VERSION=`ant -f build-swellrt.xml version | sed -n -e 's/^.*Version=//p'` 

echo "  current version is $VERSION Need docker tagging?"

docker inspect $NAME:$VERSION > /dev/null 2>&1

if [ $? -eq 1 ]
then
  echo "  Mmm, there is not a docker image with this version. Is it already git-tagged?"
    git show v$VERSION > /dev/null 2>&1

    if [ $? -eq 0 ]
    then
      echo "  Yeah! git tag present!"
      echo "Tagging latest image.."
      docker tag $NAME $NAME:$VERSION

      PUSH_TAG=1
    else
      # Try to track bug in production
      git show v$VERSION > /dev/null 2>&1
      echo $?
      echo "  git tag does not exist yet!"
    fi
else
  echo "  docker tag already exists!"
fi

echo "Publishing image.."

push_tag $NAME:latest


if [ $PUSH_TAG -eq 1 ]
then
  echo "Pushing tag.."
  push_tag $NAME:$VERSION
fi

