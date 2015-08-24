#!/bin/sh

NAME=p2pvalue/swellrt

echo "Building image.."

docker build -t $NAME .

if [ $? -ne 0 ]; then exit 1; fi

echo "Publishing image.."

docker push $NAME:latest

if [ $? -ne 0 ]; then exit 1; fi

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
      echo "Pushing tag.."
      docker push $NAME:$VERSION

      if [ $? -ne 0 ]; then exit 1; fi
      echo "  done!"
    else
      echo "  git tag does not exist yet!"
    fi
else
  echo "  docker tag already exists!"
fi
