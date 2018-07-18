CAP1XXX driver for Android Things
=================================

This driver supports capacitive touch controls build on the CAP12XX family of chips
(CAP1203, CAP1293, CAP1206, CAP1296, CAP1208 and CAP1298) and most CAP11XX family chips 
(CAP1105, CAP1106, CAP1126, CAP1128, CAP1133, CAP1166, CAP1188).

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `cap1xxx` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    implementation 'com.google.android.things.contrib:driver-cap1xxx:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx;

// Access the capacitive touch control:

Cap1xxx mCapTouchControl;

try {
    mCapTouchControl = new Cap1xxx(
            i2cBusName,                     // required I2C
            gpioPinName,                    // optional GPIO for interrupt alerts
            Cap1xxx.Configuration.CAP1208   // choose whichever matches your chip
    );
    mCapTouchControl.setOnCapTouchListener(new Cap1xxx.OnCapTouchListener() {
        @Override
        public void onCapTouchEvent(Cap1xxx controller, boolean[] inputStatus) {
            // do something awesome
        }
    });
} catch (IOException e) {
    // couldn't configure the touch control...
}

// Close the capacitive touch control when finished:

try {
    mCapTouchControl.close();
} catch (IOException e) {
    // error closing servo
}
```

Instead of listening to touches directly, you can register the capacitive touch control
with the system and receive `KeyEvent`s using the standard Android APIs:

```java
int[] keyCodes = new int[] {
        KevEvent.KEYCODE_1, KevEvent.KEYCODE_2, ... KevEvent.KEYCODE_8
};
Cap1xxxInputDriver mInputDriver;

try {
    mInputDriver = new Cap1xxxInputDriver(
            this,                           // context
            i2cBusName,
            null,
            Cap1xxx.Configuration.CAP1208,  // 8 input channels
            keyCodes                        // keycodes mapped to input channels
    );

    // Disable repeated events
    mInputDriver.setRepeatRate(Cap1xxx.REPEAT_DISABLE);
    // Block touches above 4 unique inputs
    mInputDriver.setMultitouchInputMax(4);

    mInputDriver.register();
} catch (IOException e) {
    // couldn't configure the input driver...
}

// Override key event callbacks in your Activity:

@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
        case KeyEvent.KEYCODE_1:
            doSomethingAwesome();
            return true; // handle keypress
        // other cases...
    }
    return super.onKeyDown(keyCode, event);
}

// Unregister and close the input driver when finished:

mInputDriver.unregister();
try {
    mInputDriver.close();
} catch (IOException e) {
    // error closing input driver
}
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-cap1xxx/_latestVersion
