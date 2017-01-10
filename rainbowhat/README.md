Rainbow Hat driver for Android Things
=====================================

This driver provides easy access to the peripherals available on the [Rainbow Hat for Android
Things][product]:
- BMP280 temperature sensor (I2C)
- HT16K33 segment display (I2C)
- Capacitive buttons (GPIO)
- LEDs (GPIO)
- APA102 RGB LEDs (SPI)
- Piezo Buzzer (PWM)
- Servo header (PWM)


NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.


How to use the driver
---------------------

### Gradle dependency

To use the `rainbowhat` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-rainbowhat:<version>'
}
```

### Sample usage


```java
// import the RainbowHat driver
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
```

```java
// Light up the Red LED.
Gpio led = RainbowHat.openLed(RainbowHat.LED_RED);
led.setValue(true);
// Close the device when done.
led.close();
```

```java
// Display a string on the segment display.
AlphanumericDisplay segment = RainbowHat.openDisplay();
segment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
segment.display("ABCD");
segment.setEnabled(true);
// Close the device when done.
segment.close();
```

```java
// Play a note on the buzzer.
Speaker buzzer = RainbowHat.openBuzzer();
buzzer.play(440);
// Stop the buzzer.
buzzer.stop();
// Close the device when done.
buzzer.close();
```

```java
// Log the current temperature
Bmx280 sensor = RainbowHat.openSensor();
sensor.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
Log.d(TAG, "temperature:" + sensor.readTemperature());
// Close the device when done.
sensor.close();
```

```java
// Display the temperature on the segment display.
Bmx280 sensor = RainbowHat.openSensor();
sensor.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
AlphanumericDisplay segment = RainbowHat.openDisplay();
segment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
segment.display(sensor.readTemperature());
segment.setEnabled(true);
// Close the devices when done.
sensor.close();
segment.close();
```

```java
// Light up the rainbow
Apa102 ledstrip = RainbowHat.openLedStrip();
ledstrip.setBrightness(31);
int[] rainbow = new int[RainbowHat.LEDSTRIP_LENGTH];
for (int i = 0; i < rainbow.length; i++) {
    rainbow[i] = Color.HSVToColor(255, new float[]{i * 360.f / rainbow.length, 1.0f, 1.0f});
}
ledstrip.write(rainbow);
// Close the device when done.
ledstrip.close();
```

```java
// Detect button press.
Button button = RainbowHat.openButton(RainbowHat.BUTTON_A);
button.setOnButtonEventListener(new Button.OnButtonEventListener() {
    @Override
    public void onButtonEvent(Button button, boolean pressed) {
        Log.d(TAG, "button A pressed:" + pressed);
    }
});
// Close the device when done.
button.close();
```

```java
// Get native Android 'A' key events when button 'A' is pressed.
ButtonInputDriver inputDriver = RainbowHat.createButtonInputDriver(
        RainbowHat.BUTTON_A,    // button on the hat
        KeyEvent.KEYCODE_A      // keyCode to send
);
inputDriver.register();

// In your Activity.
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_A) {
        // ...
    }
    return super.onKeyDown(keyCode, event);
}
@Override
public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_A) {
        // ...
    }
    return super.onKeyUp(keyCode, event);
}
```

```java
// Continously report temperature.
final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
sensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
    @Override
    public void onDynamicSensorConnected(Sensor sensor) {
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            sensorManager.registerListener(
                    new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            Log.i(TAG, "sensor changed: " + event.values[0]);
                        }
                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                            Log.i(TAG, "accuracy changed: " + accuracy);
                        }
                    },
                    sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
});
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

[product]: https://shop.pimoroni.com/products/rainbow-hat-for-android-things
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-rainbowhat
