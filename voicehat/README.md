Voice Hat driver for Android Things
=====================================

This driver exposes [I2S](https://developer.android.com/things/reference/com/google/android/things/pio/I2sDevice.html)
microphone input and audio output from a [VoiceHat](https://aiyprojects.withgoogle.com/voice/) to
standard Android audio APIs using the Android Things [AudioInputDriver](https://developer.android.com/things/reference/com/google/android/things/userdriver/AudioInputDriver.html)
and the [AudioOutputDriver](https://developer.android.com/things/reference/com/google/android/things/userdriver/AudioOutputDriver.html).

At this moment the driver has only been tested on the Raspberry Pi 3.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.


How to use the driver
---------------------

### Gradle dependency

To use the `voicehat` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-voicehat:<version>'
}
```

In your AndroidManifest, add the following three permissions:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="com.google.android.things.permission.MANAGE_AUDIO_DRIVERS" />
```

### Sample usage


```java
// import the VoiceHat driver
import com.google.android.things.contrib.driver.voicehat.VoiceHatDriver;
```

```java
// Start voice hat.
mVoiceHatDriver = new VoiceHatDriver(
    "I2S1",
    "BCM16",
    AUDIO_FORMAT_STEREO
  );
mVoiceHatDriver.registerAudioInputDriver();
mVoiceHatDriver.registerAudioOutputDriver();

// Make sure to call mVoiceHatDriver.close() in the onDestroy() method in your activity
```

```java
// Start recording audio.
mAudioRecord = new AudioRecord.Builder()
    .setAudioSource(MediaRecorder.AudioSource.MIC)
    .setAudioFormat(AUDIO_FORMAT_IN_MONO)
    .setBufferSizeInBytes(inputBufferSize)
    .build();

ByteBuffer audioData = ByteBuffer.allocateDirect(SAMPLE_BLOCK_SIZE);
int result =
    mAudioRecord.read(audioData, audioData.capacity(), AudioRecord.READ_BLOCKING);
if (result < 0) {
    Log.e(TAG, "error reading from audio stream:" + result);
    return;
}
// Audio data stored in `audioData`.
```

```java
// Play audio
mAudioTrack = new AudioTrack.Builder()
    .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
    .setBufferSizeInBytes(outputBufferSize)
    .build();
mAudioTrack.play();

ByteBuffer audioData = Example.getSampleAudioData(SAMPLE_BLOCK_SIZE);
mAudioTrack.write(audioData, audioData.remaining(), AudioTrack.WRITE_BLOCKING);
```

License
-------

Copyright 2016 Google Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-voicehat