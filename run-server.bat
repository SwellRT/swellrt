echo off
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