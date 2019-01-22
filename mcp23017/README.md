CAP1XXX driver for Android Things
=================================

This driver supports 16 input/output port expander MCP23017

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `mcp23017` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    implementation 'com.google.android.things.contrib:driver-mcp23017:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.mcp23017.MCP23017;
import com.google.android.things.contrib.driver.mcp23017.MCP23017GPIO;
import com.google.android.things.pio.Gpio;

// Access the IO expander:

MCP23017 mcp23017;

try {
    mcp23017 = new MCP23017(
        i2cBusName,    // required I2C
        address        // address of MCP23017
    );
} catch (IOException e) {
    // couldn't configure the IO expander
}

try {
    Gpio gpio = mcp23017.openGpio(MCP23017GPIO.A0);
} catch (
// Close the IO expander when finished:

try {
    mcp23017.close();
} catch (IOException e) {
    // error closing the expander
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

Copyright 2019 Google Inc.

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
