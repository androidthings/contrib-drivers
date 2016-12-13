GPS driver for Android Things
=================================

This driver supports GPS peripherals that generate NMEA location sentences over UART.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `gps` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-gps:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.gps.NmeaGpsModule;

// Access the GPS module:

NmeaGpsModule mGpsModule;

try {
    mGpsModule = new NmeaGpsModule(
            this,           // context
            uartPortName,
            baudRate        // specified baud rate for your GPS peripheral
    );
    mGpsModule.setGpsAccuracy(accuracy); // specified accuracy for your GPS peripheral
    mGpsModule.setGpsModuleCallback(new GpsModuleCallback() {
        // overridden methods
    });
} catch (IOException e) {
    // couldn't configure the gps module...
}

// Close the GPS module when finished:

try {
    mGpsModule.close();
} catch (IOException e) {
    // error closing gps module
}
```

Instead of reading location directly, you can register the GPS module with the system and receive
location updates using the [Location APIs][location]:
```java
NmeaGpsDriver mGpsDriver;

try {
    mGpsDriver = new NmeaGpsDriver(
            this,           // context
            uartPortName,
            baudRate,       // specified baud rate for your GPS peripheral
            accuracy        // specified accuracy for your GPS peripheral
    );

    mGpsDriver.register();

    // At this point you can get the current location and request updates using the location APIs
    // provided by Google Play Services (or LocationManager if you don't have Play Services).
} catch (IOException e) {
    // couldn't configure the gps driver...
}

// Unregister and close the input driver when finished:

mGpsDriver.unregister;
try {
    mGpsDriver.close();
} catch (IOException e) {
    // error closing gps driver
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-gps/_latestVersion
[location]: https://developer.android.com/training/location/index.html
