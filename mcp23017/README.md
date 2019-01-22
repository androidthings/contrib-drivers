MCP23017 driver for Android Things
=================================

This driver supports 16 input/output port expander MCP23017

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `mcp23017` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    implementation 'com.google.android.things.contrib:driver-mcp23017:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.mcp23017.MCP23017;
import com.google.android.things.contrib.driver.mcp23017.MCP23017GPIO;
import com.google.android.things.pio.Gpio;

// Access the IO expander:

MCP23017 mcp23017;

try {
    mcp23017 = new MCP23017(
        i2cBusName,    // required I2C
        address        // address of MCP23017
    );
} catch (IOException e) {
    // couldn't configure the IO expander
}

// Open GPIO

Gpio gpio;

try {
    gpio = mcp23017.openGpio(MCP23017GPIO.A0);
} catch (IOException e) {
    // couldn't open GPIO
}

// Close the IO expander when finished:

try {
    mcp23017.close();
} catch (IOException e) {
    // error closing the expander
}
```

For configuration GPIO and adding callback, you can use standard Andorid APIs. 
For more information see [GPIO API](https://developer.android.com/things/sdk/pio/gpio).

!!! Important: method `onGpioError()` of `GpioCallback` is not supported in current version of the driver. 

```java
Gpio gpio;

try {
    gpio = mcp23017.openGpio(MCP23017GPIO.A0);
    gpio.setDirection(Gpio.DIRECTION_IN);
    gpio.setActiveType(Gpio.ACTIVE_HIGH);       
    gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
    gpio.registerGpioCallback(mGpioCallback);
} catch (IOException e) {
    // couldn't configure GPIO
} 

private GpioCallback mGpioCallback = new GpioCallback() {
    @Override
    public boolean onGpioEdge(Gpio gpio) {
        // Read the active low pin state
        if (gpio.getValue()) {
            // Pin is HIGH
        } else {
            // Pin is LOW
        }
        return true;
    }
};
```


License
-------

Copyright 2019 Google Inc.

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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-cap1xxx/_latestVersion
