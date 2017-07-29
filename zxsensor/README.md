ZX Sensor driver for Android Things
===================================

This driver supports ZXSensor peripherals using the I2C and UART protocols.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `zxsensor` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-zxsensor:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.zxsensor.ZxSensor;
import com.google.android.things.contrib.driver.zxsensor.ZxSensorUart;

// Access the ZXSensor (choose I2C or UART) here we show UART:

ZxSensorUart zxSensorUart;

try {
    zxSensorUart = ZxSensor.Factory.openViaUart(BoardDefaults.getUartPin());
} catch (IOException e) {
    throw new IllegalStateException("Can't open, did you use the correct pin name?", e);
}
zxSensorUart.setSwipeLeftListener(swipeLeftListener);
zxSensorUart.setSwipeRightListener(swipeRightListener);

ZxSensor.SwipeLeftListener swipeLeftListener = new ZxSensor.SwipeLeftListener() {
        @Override
        public void onSwipeLeft(int speed) {
            Log.d("TUT", "Swipe left detected");
        }
    };

ZxSensor.SwipeRightListener swipeRightListener = new ZxSensor.SwipeRightListener() {
        @Override
        public void onSwipeRight(int speed) {
            Log.d("TUT", "Swipe right detected");
        }
    };

// Start monitoring:

zxSensorUart.startMonitoringGestures();

// Stop monitoring:

zxSensorUart.stopMonitoringGestures();

// Close the ZXSensor when finished:

zxSensorUart.close();
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-zxsensor/_latestVersion
