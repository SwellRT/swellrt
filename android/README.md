# SwellRT, Android Client

This is the SwellRT sub-project offering the Android client component.



## Adding SwellRT to your Android projects

### Getting precompiled SwellRT-Android artificats

TBC

### Building the SwellRT-Android

TBC


## Devolping the SwellRT Android Client

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
