MMA7660FC driver for Android Things
===================================

This driver supports the NXP MMA7660FC [three-axis accelerometer][product].

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `mma7660fc` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-mma7660fc:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.mma7660fc.Mma7660fc;

// Access the environmental sensor directly:

mma7660fc mMma7660fc;

try {
    mMma7660fc = new Mma7660fc(i2cBusName);
    mMma7660fc.setMode(Mma7660fc.MODE_ACTIVE);
} catch (IOException e) {
    // couldn't configure the device...
}

// Read accelerometer values:

try {
    float[] sample = mMma7660fc.readSample();
} catch (IOException e) {
    // error reading sample
}

// Close the accelerometer sensor when finished:

try {
    mMma7660fc.close();
} catch (IOException e) {
    // error closing sensor
}
```

If you need to read sensor values continuously, you can register the Mma7660Fc with the system and
listen for sensor values using the [Sensor APIs][sensors]:
```java
SensorManager mSensorManager = getSystemService(Context.SENSOR_SERVICE);
SensorEventListener mListener = ...;
Mma7660FcAccelerometerDriver mSensorDriver;

mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
    @Override
    public void onDynamicSensorConnected(Sensor sensor) {
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mSensorManager.registerListener(mListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
});

try {
    mSensorDriver = new Mma7660FcAccelerometerDriver(i2cBusName);
    mSensorDriver.register();
} catch (IOException e) {
    // Error configuring sensor
}

// Unregister and close the driver when finished:

mSensorManager.unregisterListener(mListener);
mSensorDriver.unregister();
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

[product_bmp280]: http://www.nxp.com/products/sensors/accelerometers/3-axis-accelerometers/1.5g-low-g-digital-accelerometer:MMA7660FC
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-mma7660fc/_latestVersion
[sensors]: https://developer.android.com/guide/topics/sensors/sensors_overview.html
