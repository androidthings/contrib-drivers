TM1637 display driver for Android Things
========================================

This driver supports segment display peripherals built on the TM1637 chip.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `tm1637` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-tm1637:<version>'
}
```

### Sample usage

Use the `NumericDisplay` class to easily drive a 7-segment display. Use the `Tm1637` class when
driving other types of [TM1637 led matrices](led_matrices)
```java
import com.google.android.things.contrib.driver.tm1637.NumericDisplay;

// Access the display:

NumericDisplay mDisplay;

try {
    mDisplay = new NumericDisplay(dataGpioPinName, clockGpioPinName);
    mDisplay.setEnabled(true);
} catch (IOException e) {
    // couldn't configure the display...
}

// Display a number or a time:

try {
    // displays "42"
    mDisplay.display(42);

    // displays "12:00"
    mDisplay.setColonEnabled(true);
    mDisplay.display("1200");
} catch (IOException e) {
    // error setting display
}

// Close the display when finished:

try {
    mDisplay.close();
} catch (IOException e) {
    // error closing display
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-tm1637/_latestVersion
[led_matrices]: https://learn.adafruit.com/adafruit-led-backpack/connecting-multiple-backpacks?view=all
