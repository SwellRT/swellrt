
This folder contains some Selenium scripts to automatically test client side aspects of SwellRT:

- *TestWaveJS* allows to run automatically Jasmine unit tests of the JS API (see war/test)


## Setting up

Download a suitable web driver (e.g. chromedriver)
If location of the chrome driver is different from "/usr/local/bin/chromedriver" you should set the path as JVM option when you run the selenium project:

`-Ddriverpath=path/to/chromedriver`

## Development Using Eclipse IDE

Import the folder `selenium/` as "Existing Java Project" into Eclipse
Customize o create a selenium script. Check out `selenium/TestWaveJS.java` as reference.






