ADC Drivers for Android Things
====================================

This driver supports the following analog to digital converters (ADCs):

| Driver | ADC | Module |
| :----: | --- | ------ |
| MCP3xxx | MCP3002 | [SparkFun](https://www.sparkfun.com/products/8636)
| MCP3xxx | MCP3004 |
| MCP3xxx | MCP3008 | [Adafruit](https://www.adafruit.com/product/856)
| ADS1xxx | ADS1013, ADS1014, ADS1015 | [Adafruit](https://www.adafruit.com/product/1083)
| ADS1xxx | ADS1113, ADS1114, ADS1115 | [Adafruit](https://www.adafruit.com/product/1085)

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `adc` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-adc:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.adc.ads1xxx.Ads1xxx;

try {
    // Connect to an ADS1015 using the default I2C address
    mAdcDriver = new Ads1xxx(i2cBusName, Ads1xxx.Configuration.ADS1015);
    // Increase default range to fit +3.3V
    mAdcDriver.setInputRange(Ads1xxx.RANGE_4_096V);
} catch (IOException e) {
    // couldn't configure the device...
}

// Elsewhere in a loop
try {
    // Get the voltage difference between IN0+ and IN1-
    float voltage = mAdcDriver.readDifferentialVoltage(Ads1xxx.INPUT_DIFF_0P_1N);
} catch (IOException e) {
    // error reading result
}

// Close the block when finished:
try {
    mAdc.close()
} catch (IOException e) {
    // error closing
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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-adc/_latestVersion
