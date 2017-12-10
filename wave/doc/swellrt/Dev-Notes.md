

### Wave client and multithreading 

Original wave client source code, which is targeted for browser runtime environment, is used almost equally
for the pure java wave client. This fact brings concerns about multithreading, in particular how Java Websocket libraries integrates with rest of Wave client code.

In the simplest scenario, a Java app runs in a single thread, so Wave client also runs in that thread.
What happens when the Websocket receives new data to update the Wave client's state? Is it safe for the Websocket library update the Wave client's state?

   