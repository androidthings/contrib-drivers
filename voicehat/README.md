Voice Hat driver for Android Things
=====================================

This driver exposes peripherals from the [VoiceHat](https://aiyprojects.withgoogle.com/voice/) to
standard Android audio APIs using Android Things.

It allows the user to interact with:

* Pushbutton
* LED
* Max98357A DAC

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

### Sample usage


```java
// import the VoiceHat driver
import com.google.android.things.contrib.driver.voicehat.VoiceHat;
```

### Access Max98357A DAC
The VoiceHat uses the [Max98357A](https://datasheets.maximintegrated.com/en/ds/MAX98357A-MAX98357B.pdf)
chip for audio I/O. To access the DAC, one can use the static method on the VoiceHat.
On the VoiceHat, the gain slot pin is not electrically connected.

```java
Max98357A dac = VoiceHat.openDac();
```

If you are not using the VoiceHat, you can directly construct an instance of this class.

```java
Max98357A dac = new Max98357A("BCM16", "BCM23");
dac.setSdMode(Max98357A.SD_MODE_LEFT);
dac.setGainSlot(Max98357A.GAIN_SLOT_ENABLE);
```

### Integrating Peripherals
You can access the additional peripherals on the VoiceHat: a pushbutton and an LED.

```java
Gpio led = VoiceHat.openLed();
led.setValue(true);

Button button = VoiceHat.openButton();
button.setOnButtonEventListener(new OnButtonEventListener() {
     @Override
     public void onButtonEvent(Button button, boolean pressed) {
         // do something awesome
     }
});
```

Alternatively you can register an InputDriver for the pushbutton.

**Note**: If you are going to use the input driver, you will need to include the following permission
to your AndroidManifest:

```xml
<uses-permission android:name="com.google.android.things.permission.MANAGE_INPUT_DRIVERS" />
```

```java
// Start voice hat.
UserDriverManager userDriverManager = UserDriverManager.getManager();
ButtonInputDriver buttonInputDriver = VoiceHat.createButtonInputDriver(KeyEvent.KEYCODE_ENTER);
userDriverManager.registerInputDriver(buttonInputDriver);

// Make sure to call `unregisterInputDriver` in the onDestroy() method in your activity
```

License
-------

Copyright 2017 Google Inc.

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