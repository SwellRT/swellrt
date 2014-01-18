echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

if not exist "server.config" (
  echo "You need to copy server.config.example to server.config and edit it. Or run: 'ant -f server-config.xml' to generate the file automatically."
  pause
  exit 1
)

for /F "tokens=1* delims==" %%A IN (build.properties) DO (
    IF "%%A"=="waveinabox.version" set WAVEINABOX_VERSION=%%B
    IF "%%A"=="name" set NAME=%%B
    )
echo on

java -Djava.util.logging.config.file=wiab-logging.conf -Djava.security.auth.login.config=jaas.config -Dwave.server.config=server.config  -jar dist/%NAME%-server-%WAVEINABOX_VERSION%.jar
pause
