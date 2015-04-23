

## Setting up

* Download the suitable driver (e.g. chromedriver)

## Development Using Eclipse IDE

Import the folder `selenium/` as Existing Java Project into Eclipse
Edit `selenium/TestWaveJS.java`.

## Build

Compile with Ant: `ant -f build-swellrt.xml swellrt-compile-js-dev`

## Run

Build a run Wave Server separately: `ant compile run-server`

The first time you run the test you should register the test user at http://localhost:9898/auth/register with name 'test' and password 'test'

Run test: `ant -f build-swellrt.xml swellrt-js-test`

Note: if the location of the chrome driver is different from "/usr/local/bin/chromedriver" you should set the path as JVM option as:

 `ant -f build-swellrt.xml swellrt-js-test -Ddriverpath=path/to/chromedriver`




