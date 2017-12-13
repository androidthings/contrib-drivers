LoWPAN driver for Android Things
=================================

This driver supports UART-based LoWPAN peripherals that use the [Spinel][spinel]
protocol with the [recommended UART framing mechanism][spinelframe]. This is the
communications protocol supported by Network Co-Processor (NCP) devices running
[OpenThread][openthread].

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

Hardware support
----------------

This driver has been tested with supported [OpenThread][openthread] developer
boards running the [pre-built NCP firmware images][firmware]. We recommend the
[Nordic nRF52840-PDK][nordic] to get started.

For more details on building your own LoWPAN drivers to support additional
hardware, see the [API guide][lowpan].

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
    // error closing lowpan driver
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

[spinel]: https://tools.ietf.org/html/draft-rquattle-spinel-unified
[spinelframe]: https://tools.ietf.org/html/draft-rquattle-spinel-unified-00#appendix-A.1.2
[openthread]: https://openthread.io
[firmware]: https://openthread.io/guides/ncp/firmware
[nordic]: https://www.nordicsemi.com/eng/Products/nRF52840-Preview-DK
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-lowpan/_latestVersion
[lowpan]: https://developer.android.com/things/sdk/drivers/lowpan.html
