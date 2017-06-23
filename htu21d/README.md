HTU21D driver for Android Things
================================

This driver supports TE Connectivity [HTU21D][product_htu21d] environmental sensor.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `htu21d` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-htu21d:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.htu21d.Htu21d;

// Access the environmental sensor:

Htu21d mHtu21d;

try {
    mHtu21d = new Htu21d(i2cBusName);
} catch (IOException e) {
    // couldn't configure the device...
}

// Read the current temperature:

try {
    float temperature = mHtu21d.readTemperature();
} catch (IOException e) {
    // error reading temperature
}

// Close the environmental sensor when finished:

try {
    mHtu21d.close();
} catch (IOException e) {
    // error closing sensor
}
```

If you need to read sensor values continuously, you can register the Htu21d with the system and
listen for sensor values using the [Sensor APIs][sensors]:
```java
SensorManager mSensorManager = getSystemService(Context.SENSOR_SERVICE);
SensorEventListener mListener = ...;
Htu21dSensorDriver mSensorDriver;

mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
    @Override
    public void onDynamicSensorConnected(Sensor sensor) {
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            mSensorManager.registerListener(mListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
});

try {
    mSensorDriver = new Htu21dSensorDriver(i2cBusName);
    mSensorDriver.registerTemperatureSensor();
} catch (IOException e) {
    // Error configuring sensor
}

// Unregister and close the driver when finished:

mSensorManager.unregisterListener(mListener);
mSensorDriver.unregisterTemperatureSensor();
try {
    mSensorDriver.close();
} catch (IOException e) {
    // error closing sensor
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

[product_htu21d]: http://www.te.com/usa-en/product-CAT-HSC0004.html
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-htu21d/_latestVersion
[sensors]: https://developer.android.com/guide/topics/sensors/sensors_overview.html
