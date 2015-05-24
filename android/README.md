# SwellRT Android Client

This is the SwellRT sub-project providing the Android native client component.



## Adding SwellRT to your Android projects

### Building the SwellRT Android artificats

- Configure Android SDK classpath in **swellrt/build-swellrt.properties**
- Get third-party libraries: `ant -f build-swellrt.xml get-third-party-swellrt-android`. They will be put into **swellrt/android/libs**
- Build and generate client components: `ant -f build-swellrt.xml dist-swellrt-android`. They will be put into **swellrt/android/dist**

List of generated artifacts::

- **swellrt/android/dist/swellrt-android-X.Y.jar**: Android-specific client part.
- **swellrt/android/libs/swellrt-android-dep-X.Y.jar**: Pure-Java components shared with the server project.


### Setting up an Android project with SwellRT

- Add all generated and third-party dependencies pointed in the previous section to you Android project's build path.

## Using the SwellRT Android service

Edit your **AndroidManifest.xml** adding the following service definition to the *application* section:

```
<manifest>
    <application>
        <service android:name="org.swellrt.android.service.SwellRTService"></service>
    </application>
</manifest>
```


Use the SwellRTService as **Bound service** in your activities:

```
final Intent mWaveServiceIntent = new Intent(this, SwellRTService.class);
bindService(mWaveServiceIntent, this, Context.BIND_AUTO_CREATE);
```

Implement the **ServiceConnection** interface to get service's reference (*SwellRTService*):

```
 @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    SwellRTService mSwellRT = ((SwellRTService.SwellRTBinder) service).getService(this);
    Log.d(this.getClass().getSimpleName(), "SwellRT Service Bound");

   (...)
  }
```

### SwellRTService

The SwellRT Service provides same operations than the JavaScript API client. Please see the main README file for more information.
This is a summary of operations and service callbacks.


**public void startSession(String serverURL, String username, String password)**

Open a session with the SwellRT/Wave server for the passed credentials.
The service only operates with one open session at once. It is shared accross activities in your application.

**public void stopSession()**

Stop the already open session, cleaning up all communication resources and closing all opened data models (waves).


**public void openModel(String modelId)**

Open a model with the passed id. The result is provided asyncronously in **SwellRTServiceCallback.onOpen(Model)**.

**public String createModel()**

Create a new model. The result is provided asyncronously in **SwellRTServiceCallback.onCreate(Model)**.

**public Model getModel(String modelId)**

This method allows you to get an already open data model created or opened in a different activity.
The service keeps track of all open data models until they are closed individually or when session is stopped.

**public void closeModel(String modelId)**

Close the data model. The result is provided asyncronously in **SwellRTServiceCallback.oClose()**.









## Developing the SwellRT Android Client

### Eclipse

1. Download the **swellrt** source code

2. Download the **swellrt-android** wrapper project

3. Setup up **swellrt-android** wrapper project

You must link the Androind service source code folder locate at **swellrt/android**
as a source code in the **swellrt-android**: go to Project -> Properties -> Java Build Path -> Source -> Link Source...

Edit your **AndroidManifest.xml** adding the following service definition to the *application* section:

```
<manifest>
    <application>
        <service android:name="org.swellrt.android.service.SwellRTService"></service>
    </application>
</manifest>
```

Add additional *.jar's* from **swellrt/android/libs**: go to Project -> Properties -> Java Build Path -> Libraries -> Add JARs...
