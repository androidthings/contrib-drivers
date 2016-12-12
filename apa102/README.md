APA102 LED driver for Android Things
====================================

This driver supports RGB LED peripherals built on the APA102 SPI protocol.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `apa102` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-apa102:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.apa102.Apa102;

// Access the LED strip:

Apa102 mApa102;

try {
    mApa102 = new Apa102(spiBusName, Apa102.Mode.RGB);
} catch (IOException e) {
    // couldn't configure the device...
}

// Light it up!

int[] colors = new int[] {Color.RED, Color.GREEN, Color.BLUE};
try {
    mApa102.write(colors);
} catch (IOException e) {
    // error setting LEDs
}

// Close the LED strip when finished:

try {
    mApa102.close();
} catch (IOException e) {
    // error closing LED strip
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-apa102/_latestVersion
