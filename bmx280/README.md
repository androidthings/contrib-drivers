BMX280 driver for Android Things
================================

This driver supports Bosch [BMP280][product_bmp280] and [BME280][product_bme280]
environmental sensors.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `bmx280` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-bmx280:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.bmx280.Bmx280;

// Access the environmental sensor:

Bmx280 mBmx280;

try {
    mBmx280 = new Bmx280(i2cBusName);
    mBxm280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
} catch (IOException e) {
    // couldn't configure the device...
}

// Read the current temperature:

try {
    float temperature = mBmx280.readTemperature();
} catch (IOException e) {
    // error reading temperature
}

// Close the environmental sensor when finished:

try {
    mBmx102.close();
} catch (IOException e) {
    // error closing sensor
}
```

If you need to read sensor values continuously, you can register the Bmx280 with the system and
listen for sensor values using the [Sensor APIs][sensors]:
```java
SensorManager mSensorManager = getSystemService(Context.SENSOR_SERVICE);
SensorEventListener mListener = ...;
Bmx280SensorDriver mSensorDriver;

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
    mSensorDriver = new Bmx280SensorDriver(i2cBusName);
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

[product_bmp280]: https://www.bosch-sensortec.com/bst/products/all_products/bmp280
[product_bme280]: https://www.bosch-sensortec.com/bst/products/all_products/bme280
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-bmx280/_latestVersion
[sensors]: https://developer.android.com/guide/topics/sensors/sensors_overview.html
