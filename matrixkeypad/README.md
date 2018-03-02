Matrix Keypad driver for Android Things
=====================================

This driver allows a matrix keypad, such as the [Membrane Matrix Keypad from Adafruit](https://www.adafruit.com/product/419),
 to connect to Android Things using [GPIO](https://developer.android.com/things/sdk/pio/gpio.html)
for a keypad of any number of rows and columns. There will be one GPIO pin for each row and column, and this driver uses standard
Android APIs to emit `onKeyDown` and `onKeyUp` events based on the user's actions.

How does it work?
-----------------
A matrix keypad is designed so each column and row has a specific key. When that key is pressed, a
signal is able to travel from the column through the row and register as logic HIGH on the Android
Things device. This is matched to the corresponding key at that column and row.

To accomplish this, a thread is running which iterates through every column. It sets one column at
a time as logic HIGH and checks for the values of each row. Then it sets that column as an input and
checks the next column.

By setting the columns not being checked as input, it sets those wires to high impedance and prevents
a potential short if two keys are pressed at the same time.

This loop occurs on a different thread in order to prevent the driver from blocking the main thread.

At this moment the driver has only been tested on the Raspberry Pi 3.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

Wiring Note
-----------
It is highly recommended to use pull-down resistors on the row pins. This will prevent any signals from being misread
by the driver as false positives.

How to use the driver
---------------------

### Gradle dependency

To use the `matrixkeypad` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-matrixkeypad:<version>'
}
```

In your AndroidManifest, add the following permission:

```xml
<uses-permission android:name="com.google.android.things.permission.MANAGE_INPUT_DRIVERS" />
```

### Sample usage


```java
// import the MatrixKeypad driver
import com.google.android.things.contrib.driver.matrixkeypad.MatrixKeypadInputDriver;
```

In our example, we will be using the [Membrane Matrix Keypad from Adafruit](https://www.adafruit.com/product/419).
This keypad has four rows and three columns. We will provide arrays corresponding to the pins for each row,
the pins for each column, and the keycodes for each button. The keycodes should be entered from
left-to-right, top-to-bottom.

```java
// Start keypad.
mMatrixKeypadDriver = new MatrixKeypadDriver(
    new String[] {"BCM17", "BCM27", "BCM22", "BCM5"}, // 4 Rows
    new String[] {"BCM6", "BCM13", "BCM19"},          // 3 Columns
    new int[] {KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2,
                KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4,
                KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6,
                KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8,
                KeyEvent.KEYCODE_NUMPAD_9, KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
                KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_ENTER} // 12 buttons
  );
mMatrixKeypadDriver.registerKeypad();
```

The driver will start looping. When a key is pressed, the value can be obtained through the `onKeyDown`
and `onKeyUp` methods.

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
  Log.d(TAG, "Detect key down " + KeyEvent.keyCodeToString(keyCode));
  return super.onKeyDown(keyCode, event);
}
```

Alternatively, you can use a callback instead of registering the driver:

```java
// Set a callback
// Start keypad.
mMatrixKeypad = new MatrixKeypad(
    new String[] {"BCM17", "BCM27", "BCM22", "BCM5"}, // 4 Rows
    new String[] {"BCM6", "BCM13", "BCM19"},          // 3 Columns
    new int[] {KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2,
                KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4,
                KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6,
                KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8,
                KeyEvent.KEYCODE_NUMPAD_9, KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
                KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_ENTER} // 12 buttons
mMatrixKeypad.setKeyCallback(new MatrixKeyCallback() {
  @Override
  public void onKeyEvent(MatrixKey matrixKey) {
    Log.d(TAG, "Was key pressed? " + matrixKey.isPressed());
    Log.d(TAG, "Key pressed code: " + matrixKey.getKeyCode());
  }
});
```

Make sure to properly unregister the driver and close it in the `onDestroy` method in your activity:

```java
@Override
protected void onDestroy() {
  super.onDestroy();
  try {
    driver.unregisterKeypad();
    driver.close();
  } catch (IOException e) {
     ...
  }
}
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-matrixkeypad
