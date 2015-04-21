

## Setting up

* Download selenium library and dependencies from http://selenium-release.storage.googleapis.com/2.44/selenium-java-2.44.0.zip
* Uncompress and copy all files to `selenium/lib` folder.
* Download the suitable driver (e.g. chromedriver) and copy it to the `selenium/driver` folder.

## Development Using Eclipse IDE

Import the folder `selenium/` as Existing Java Project into Eclipse
Edit `selenium/TestWaveJS.java`.

## Build

Compile with Ant: `ant -f build-swellrt.xml swellrt-compile-js-dev`

## Run

Build a run Wave Server separately: `ant compile run-server`

Run test: `ant -f build-swellrt.xml swellrt-js-test`







