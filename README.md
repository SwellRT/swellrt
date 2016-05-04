# SwellRT, a Real-Time Federated Collaboration Framework

[![Join the chat at https://gitter.im/P2Pvalue/swellrt](https://badges.gitter.im/P2Pvalue/swellrt.svg)](https://gitter.im/P2Pvalue/swellrt?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

SwellRT is a collaboration framework based on [Apache Wave](http://incubator.apache.org/wave/).
It allows to develop real-time collaborative apps for **Web** (JavaScript or GWT), **Android** and **Java**.

In a nutshell, SwellRT provides to apps shared objects that can be modified by different participants on nearly real-time
distributing changes and managing concurrency.

In particular, **text objects support collaborative real-time editing.**

You can install your own server infrastructure or build on top of an existing SwellRT provider.
SwellRT servers can be federated, so your app can be deployed in a decetralized way and become interoperable easily.

## Documentation

You can visit the [SwellRT Wiki](https://github.com/P2Pvalue/swellrt/wiki) for documentation and examples.

## Licensing and third party dependencies

SwellRT is provided under the Apache License 2.0. Please check out README.md for a full list of libraries used and licenses.

SwellRT uses [Atmosphere](https://github.com/Atmosphere/atmosphere) as communication library for WebSockets and Long-Polling.



# Developing SwellRT

## Packages

| Package               |                              |                        |
|-----------------------|------------------------------|------------------------|
|org.swell.model        | Generic Wave Data Model      | Java & GWT             |
|org.swell.client       | Wave Protocol Client mods    | GWT                    |
|org.swell.webclient    | Wave UI Client mods          | GWT                    |
|org.swell.api          | JS API Generic Data Model    | GWT                    |
|org.swell.server       | Wave Java server mods        | Java                   |


### Debug (GWT Super Dev Mode)

First, launch the web server `ant run-server` serving the SwellRT javascript files.

Enable debugging of SwellRT/Apache Wave source code starting a **GWT Super Dev** session:

```
ant -f build-swellrt.xml swellrt-js-superdev
```

Then (by default) visit `http://localhost:9876` and bookmark provided links for de/activate the Dev mode.

Go to your web (e.g. http://localhost:9898/test/index.html) and activate de Dev mode with the provided link.

You can now use the browser's debugger as usual. Please, be sure your browser's debbuger has the "source maps" option activated.
Chrome is recommended.

For more info about GWT debugging, please visit http://www.gwtproject.org/articles/superdevmode.html


#### Debug issues

Debugging in the old hosted mode could raise the following error:

```
00:10:30,530 [ERROR] Failed to load module 'swellrt' from user agent 'Mozilla/5.0 (X11; Linux x86_64; rv:23.0) Gecko/20100101 Firefox/23.0' at localhost:36570
java.lang.AssertionError: Jso should contain method: @org.waveprotocol.wave.model.adt.ObservableElementList.Listener::onValueAdded(Ljava/lang/Object;)
at com.google.gwt.dev.shell.CompilingClassLoader$MyInstanceMethodOracle.<init>(CompilingClassLoader.java:431)
...
```
It can be avoided removing the **final** modifier from overrided methods in the **ObservableListJS** class.

