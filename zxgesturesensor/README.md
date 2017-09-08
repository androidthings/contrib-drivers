ZX Gesture Sensor Driver for Android Things
================================

This driver supports [Sparkfun ZX distance and gesture sensor](https://www.sparkfun.com/products/13162) on I2C or UART connection.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `zxgesturesensor` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-zxgesturesensor:<version>'
}
```

In your AndroidManifest, add the following permission:

```xml
<uses-permission android:name="com.google.android.things.permission.MANAGE_INPUT_DRIVERS" />
```

### Sample usage

```java
import com.google.android.things.contrib.driver.zxgesturesensor.ZXGestureSensor;

// Access the Gesture sensor and listen for gesture events:

ZXGestureSensor mSensor;

try {
    // for sensor connected to UART port use UART port name and getUartSensor instead
    mSensor = ZXGestureSensor.getUartSensor(uartPortName, new Handler());
    mSensor.setListener(new OnZXGestureSensorEventListener() {
        @Override
        public void onGestureEvent(ZXGestureSensor sensor, Gesture gesture, int param) {
            // do something awesome
        }
    });
} catch (IOException e) {
    // couldn't configure the sensor...
}

// Read other sensor values:

mSensor.getGestureDetector().getXpos(); // x(horizontal) position from -120 to 120
mSensor.getGestureDetector().getZpos(); // z(vertical) position from 0 to 240
mSensor.getGestureDetector().getGesture(); // last detected gesture

// Close the sensor when finished:

try {
    mSensor.close();
} catch (IOException e) {
    // error closing the sensor
}
```

Alternatively, you can register a `ZXGestureSensorInputDriver` with the system and receive `KeyEvent`s
through the standard Android APIs:
```java
ZXGestureSensorInputDriver mInputDriver;

try {
    // for sensor connected to I2C port use I2C port name and ConnectionType.I2C instead
    // EnumMap<Gesture, int> keyMap is a mapping from gestures to keycodes.
    mInputDriver = new ZXGestureSensorInputDriver(uartPortName,
            ZXGestureSensor.ConnectionType.UART,
            new Handler(),
            keyMap);
    //or use this for default keymap
    mInputDriver = new ZXGestureSensorInputDriver(uartPortName,
            ZXGestureSensor.ConnectionType.UART);
    mInputDriver.register();
} catch () {
    // error configuring the sensor...
}

// Override key event callbacks in your Activity:

@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        return true; // indicate we handled the event
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
    } // ...more if statements for each gesture-key mappings
    return super.onKeyDown(keyCode, event);
}

// Unregister and close the input driver when finished:

mInputDriver.unregister;
try {
    mInputDriver.close();
} catch (IOException e) {
    // error closing input driver
}
```

### Default Gesture-Keycode maps

* SWIPE_UP => DPAD_UP
* SWIPE_LEFT => DPAD_LEFT
* SWIPE_RIGHT => DPAD_RIGHT
* HOVER => ENTER
* HOVER_UP => PAGE_UP
* HOVER_LEFT => MOVE_END
* HOVER_RIGHT => MOVE_HOME

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


[jcenter]: https://bintray.com/google/androidthings/contrib-driver-zxgesturesensor/_latestVersion