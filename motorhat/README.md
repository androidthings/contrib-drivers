Motor Hat driver for Android Things
===================================

This driver provides access to and control of motor peripherals connected through the [Adafruit
DC and Stepper Motor HAT][product].

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `motorhat` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-motorhat:<version>'
}
```

### Sample usage

Use the `MotorHat` class to easily control up to 4 DC motors. Motors are indexed from 0 to 3,
corresponding to the ports of the Motor hat (`index = (port# - 1)`). Note: Support for Stepper
motors is forthcoming, so the API is likely to change.
```java
import com.google.android.things.contrib.driver.motorhat.MotorHat;

// Access the Motor HAT:

MotorHat mMotorHat;

try {
    mMotorHat = new NumericDisplay(I2cBusName);
} catch (IOException e) {
    // couldn't configure the display...
}

// Operate the motor:

try {
    // Use a value between 0 and 255
    mMotorHat.setMotorSpeed(0, 100);
    // Turn clockwise
    mMotorHat.setMotorState(0, MotorHat.MOTOR_STATE_CW);
    // Turn counter-clockwise
    mMotorHat.setMotorState(0, MotorHat.MOTOR_STATE_CCW);
    // Stop the motor
    mMotorHat.setMotorState(0, MotorHat.MOTOR_STATE_RELEASE);
} catch (IOException e) {
    // error setting motor
}

// Close when finished:

try {
    mMotorHat.close();
} catch (IOException e) {
    // error closing Motor Hat
}
```

Acknowledgments
---------------

Inspired by the [Adafruit MotorHAT Python Library][pythonlib], and the [Android Robocar][robocar]
Hackster project by Antonio Zugaldia and Halim Salameh.

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

[product]: https://www.adafruit.com/product/2348
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-motorhat/_latestVersion
[pythonlib]: https://github.com/adafruit/Adafruit-Motor-HAT-Python-Library
[robocar]: https://github.com/zugaldia/android-robocar
