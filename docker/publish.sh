NAME=p2pvalue/swellrt

echo "Building image"

docker build -t $NAME .

echo "Publishing image"

docker push $NAME

VERSION=`ant -f build-swellrt.xml version | sed -n -e 's/^.*Version=//p'` 

echo "Current version is $VERSION"

echo "Need tagging?"

docker inspect $NAME:$VERSION > /dev/null 2>&1


if [ $? -eq 1 ]
then
  echo "  there is not an image with this version"
  echo "  tagging latest image"
  docker tag $NAME $NAME:$VERSION
  echo "  pushing tag"
  docker push $NAME:$VERSION
else
  echo "  tag already exists"
fi
