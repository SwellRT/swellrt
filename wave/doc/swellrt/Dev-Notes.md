


## Debugging JavaScript client with GWT

- Use Chrome. Firefox and Chromium didn't work right last time I tried.

- Compile JS client: `./gradlew devWeb`

- Start the server: `./gradlew run`

- Start the debug code server: (the browser gets source code from it)
`./gradlew debugWeb`

- Go to `http://127.0.1.1:9876/` in Chrome, drag the two boorkmarks into the browser's bookmark bar

- Go to swellrt page (e.g. `http://127.0.0.1:9898/demo-pad.html`) and open Debug Console

- Click the bookmark "Dev On" and click "compile" button .
It connects the browser to the debug server, from this point, swellrt js is loaded dynamically from code server in the.

- Explore source code (in Java), set breakpoints... in the Debug console -> Sources tab

- If you perform changes in Java files, (from eclipse for example) you need to click "Dev On" bookmark again to recompile.





### Wave client and multithreading 

Original wave client source code, which is targeted for browser runtime environment, is used almost equally
for the pure java wave client. This fact brings concerns about multithreading, in particular how Java Websocket libraries integrates with rest of Wave client code.

In the simplest scenario, a Java app runs in a single thread, so Wave client also runs in that thread.
What happens when the Websocket receives new data to update the Wave client's state? Is it safe for the Websocket library update the Wave client's state?

   