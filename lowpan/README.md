Lowpan driver for Android Things
=================================

This driver supports UART-based LoWPAN peripherals that use
[Spinel](https://tools.ietf.org/html/draft-rquattle-spinel-unified) (Like those running
[OpenThread](http://openthread.io)) with the
[recommended UART framing mechanism](https://tools.ietf.org/html/draft-rquattle-spinel-unified-00#appendix-A.1.2).

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `lowpan` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-lowpan:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.lowpan.UartLowpanDriver;
UartLowpanDriver mLowpanDriver;

try {
    mLowpanDriver = new UartLowpanDriver(
            this,           // context
            uartPortName,
            baudRate,
            hwFlowControl
    );

    mLowpanDriver.register();

    // At this point you can access the lowpan network through the APIs
    // provided by LowpanManager.
} catch (IOException e) {
    // couldn't configure the lowpan driver...
}

// Unregister and close the input driver when finished:

mLowpanDriver.unregister();
try {
    mLowpanDriver.close();
} catch (IOException e) {
    // error closing gps driver
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-lowpan/_latestVersion
[lowpan]: https://developer.android.com/training/lowpan/index.html
