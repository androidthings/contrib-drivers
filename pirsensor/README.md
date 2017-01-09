PIRSensor (Passive InfraRed) driver for Android Things
================================

This driver supports GPIO Input from a PIR Sensor.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `PIRSensor` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-pirsensor:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.pirsensor.PIRSensor;

// Access the PIR Sensor and listen for events:

PIRSensor mSensor;

try {
    mSensor = new PIRSensor(gpioPinName);
    mSensor.setOnPIREventListener(new OnPIREventListener() {
        @Override
        public void onSensorEvent(PIRSensor sensor, boolean detected) {
            // do something awesome
        }
    });
} catch (IOException e) {
    // couldn't configure the sensor...
}

// Close the PIRSensor when finished:

try {
    mSensor.close();
} catch (IOException e) {
    // error closing sensor
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-pirsensor/_latestVersion
