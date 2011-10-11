#!/bin/bash
#
# Simply utility to setup an eclipse project
# Author: danilatos@google.com

set -e

if [ ! -d 'src' -o ! -d 'test' ]; then
  echo 'Must run from root directory of checkout'
  exit 1
fi

HERE=`pwd`
NAME=`basename "$HERE"`
echo "Creating .project"
sed -e "s/PROJECT_NAME/WPL-$NAME/" .project_template > .project

for f in tools/eclipse-launch/*; do
  dest=`echo \`basename "$f"\` | sed -e 's|\.|/|g;s|-|.|g'`
  echo "Creating $dest"
  cp "$f" "$dest"
done
