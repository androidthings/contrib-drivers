HTS221 driver for Android Things
================================

This driver supports STMicroelectronics [HTS221][product_hts221] capacitive digital sensor for
relative humidity and temperature.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `lps25h` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-lps25h:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.hts221.Lps25h;

// Access the environmental sensor:

Hts221 mHts221;

try {
    mHts221 = new Hts221(i2cBusName);
} catch (IOException e) {
    // Couldn't configure the device...
}

// Read the current humidity:

try {
    float humidity = mHts221.readHumidity();
} catch (IOException e) {
    // Error reading humidity
}

// Read the current temperature:

try {
    float temperature = mHts221.readTemperature();
} catch (IOException e) {
    // Error reading temperature
}

// Close the environmental sensor when finished:

try {
    mHts221.close();
} catch (IOException e) {
    // Error closing sensor
}
```

If you need to read sensor values continuously, you can register the Hts221 with the system and
listen for sensor values using the [Sensor APIs][sensors]:

```java
SensorManager mSensorManager = getSystemService(Context.SENSOR_SERVICE);
SensorEventListener mHumidityListener = ...;
SensorEventListener mTemperatureListener = ...;
Hts221SensorDriver mSensorDriver;

mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
    @Override
    public void onDynamicSensorConnected(Sensor sensor) {
        if (sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            mSensorManager.registerListener(mHumidityListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            mSensorManager.registerListener(mTemperatureListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
});

try {
    mSensorDriver = new Hts221SensorDriver(i2cBusName);
    mSensorDriver.registerHumiditySensor();
    mSensorDriver.registerTemperatureSensor();
} catch (IOException e) {
    // Error configuring sensor
}

// Unregister and close the driver when finished:

mSensorManager.unregisterListener(mHumidityListener);
mSensorManager.unregisterListener(mTemperatureListener);
mSensorDriver.unregisterHumiditySensor();
mSensorDriver.unregisterTemperatureSensor();
try {
    mSensorDriver.close();
} catch (IOException e) {
    // Error closing sensor
}
```

License
-------

Copyright 2016 Macro Yau

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

[product_hts221]: http://www.st.com/en/mems-and-sensors/lps25h.html
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-lps25h/_latestVersion
[sensors]: https://developer.android.com/guide/topics/sensors/sensors_overview.html
