#!/bin/bash

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Script to fetch and build the third-party packages that we depend
# on.

set -e

get() {
  urlbase=$1
  filename="$2"
  echo "fetching $urlbase$filename"
  wget -nv -O "$filename" "$urlbase$filename"
}

dir() {
  mkdir -p $1
  cd $1
}

mkdir -p out
out="$(pwd)/third_party/test"

[[ -f $out/emma/emma.jar ]] || (
  dir emma
  get http://repo1.maven.org/maven2/emma/emma/2.0.5312/ emma-2.0.5312.jar
  mkdir -p $out/emma
  cp emma-2.0.5312.jar $out/emma/emma.jar
)

[[ -f $out/emma/emma_ant.jar ]] || (
  dir emma
  get http://repo1.maven.org/maven2/emma/emma_ant/2.1.5320/ emma_ant-2.1.5320.jar
  mkdir -p $out/emma
  cp emma_ant-2.1.5320.jar $out/emma/emma_ant.jar
  cd ..
  rm -rf emma
)

[[ -f $out/junit/junit.jar ]] || (
  dir junit
  get http://cloud.github.com/downloads/KentBeck/junit/ junit4.10.zip
  unzip -o -q junit4.10.zip
  mkdir -p $out/junit
  cp junit4.10/junit-4.10.jar $out/junit/junit.jar
  cp junit4.10/junit-4.10-src.jar $out/junit/src.jar
  cd ..
  rm -rf junit
)

rm -rf out