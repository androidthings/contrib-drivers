Android Things user-space drivers
=================================

Sample peripheral drivers for Android Things.

> **Note:** The Android Things Console will be turned down for non-commercial
> use on January 5, 2022. For more details, see the
> [FAQ page](https://developer.android.com/things/faq).

How to use a driver
===================

For your convenience, drivers in this repository are also published to JCenter
as Maven artifacts. Look at their artifact and group ID in their build.gradle
and add them as dependencies to your own project.

For example, to use the `button` driver, version `1.0`, simply add the line
below to your project's `build.gradle`:


```
dependencies {
    implementation 'com.google.android.things.contrib:driver-button:1.0'
}
```


Current contrib drivers
-----------------------

<!-- DRIVER_LIST_START -->
Driver | Type | Usage (add to your gradle dependencies) | Note
:---:|:---:| --- | ---
[driver-adc](adc) | Analog to Digital Converters | `implementation 'com.google.android.things.contrib:driver-adc:2.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/ads1015) [changelog](adc/CHANGELOG.md)
[driver-apa102](apa102) | RGB LED strip | `implementation 'com.google.android.things.contrib:driver-apa102:1.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/apa102) [changelog](apa102/CHANGELOG.md)
[driver-bmx280](bmx280) | temperature, pressure and humidity sensor | `implementation 'com.google.android.things.contrib:driver-bmx280:1.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/bmx280) [changelog](bmx280/CHANGELOG.md)
[driver-button](button) | push button over GPIO | `implementation 'com.google.android.things.contrib:driver-button:1.0'` | [sample](https://github.com/androidthings/sample-button) [changelog](button/CHANGELOG.md)
[driver-cap1xxx](cap1xxx) | capacitive touch buttons | `implementation 'com.google.android.things.contrib:driver-cap1xxx:2.0'` |  [changelog](cap1xxx/CHANGELOG.md)
[driver-gps](gps) | GPS | `implementation 'com.google.android.things.contrib:driver-gps:1.1'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/gps) [changelog](gps/CHANGELOG.md)
[driver-ht16k33](ht16k33) | 7-digit alphanumeric segment display | `implementation 'com.google.android.things.contrib:driver-ht16k33:1.1'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/ht16k33) [changelog](ht16k33/CHANGELOG.md)
[driver-lowpan](lowpan) | LoWPAN support over UART | `implementation 'com.google.android.things.contrib:driver-lowpan:1.0'` | [sample](https://github.com/androidthings/sample-lowpan) [changelog](lowpan/CHANGELOG.md)
[driver-matrixkeypad](matrixkeypad) | driver for generic matrix keypads | `implementation 'com.google.android.things.contrib:driver-matrixkeypad:1.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/matrixkeypad) [changelog](matrixkeypad/CHANGELOG.md)
[driver-mma7660fc](mma7660fc) | accelerometer sensor | `implementation 'com.google.android.things.contrib:driver-mma7660fc:1.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/mma7660fc) [changelog](mma7660fc/CHANGELOG.md)
[driver-motorhat](motorhat) | DC Motor HAT | `implementation 'com.google.android.things.contrib:driver-motorhat:1.0'` | [sample](https://github.com/androidthings/robocar) [changelog](motorhat/CHANGELOG.md)
[driver-pwmservo](pwmservo) | PWM servo | `implementation 'com.google.android.things.contrib:driver-pwmservo:1.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/pwmservo) [changelog](pwmservo/CHANGELOG.md)
[driver-pwmspeaker](pwmspeaker) | PWM speaker/buzzer | `implementation 'com.google.android.things.contrib:driver-pwmspeaker:1.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/pwmspeaker) [changelog](pwmspeaker/CHANGELOG.md)
[driver-rainbowhat](rainbowhat) | metadriver for the Rainbow HAT | `implementation 'com.google.android.things.contrib:driver-rainbowhat:1.0'` | [sample](https://github.com/androidthings/weatherstation) [changelog](rainbowhat/CHANGELOG.md)
[driver-sensehat](sensehat) | metadriver for the Sense HAT | `implementation 'com.google.android.things.contrib:driver-sensehat:1.0'` |  [changelog](sensehat/CHANGELOG.md)
[driver-ssd1306](ssd1306) | OLED display | `implementation 'com.google.android.things.contrib:driver-ssd1306:1.1'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/ssd1306) [changelog](ssd1306/CHANGELOG.md)
[driver-thermalprinter](thermalprinter) | Driver for a thermal printer | `implementation 'com.google.android.things.contrib:driver-thermalprinter:0.1'` |  [changelog](thermalprinter/CHANGELOG.md)
[driver-tm1637](tm1637) | 4-digit numeric segment display | `implementation 'com.google.android.things.contrib:driver-tm1637:1.0'` | [sample](https://github.com/androidthings/drivers-samples/tree/master/tm1637) [changelog](tm1637/CHANGELOG.md)
[driver-vcnl4200](vcnl4200) | proximity and ambient light sensor | `implementation 'com.google.android.things.contrib:driver-vcnl4200:1.0'` |  [changelog](vcnl4200/CHANGELOG.md)
[driver-voicehat](voicehat) | driver for VoiceHat | `implementation 'com.google.android.things.contrib:driver-voicehat:1.0'` | [sample](https://github.com/androidthings/sample-googleassistant) [changelog](voicehat/CHANGELOG.md)
[driver-zxgesturesensor](zxgesturesensor) | zx gesture sensor | `implementation 'com.google.android.things.contrib:driver-zxgesturesensor:1.0'` |  [changelog](zxgesturesensor/CHANGELOG.md)
<!-- DRIVER_LIST_END -->


Deprecated drivers
------------------
Driver | Type | Usage (add to your gradle dependencies) | Reason |
:---:|:---:| --- | --- |
driver-adcv2x | Sparkfun ADC Block for Intel Edison | `implementation 'com.google.android.things.contrib:driver-adcv2x:1.0'` | Superseded by [driver-adc](adc)
driver-cap12xx | capacitive touch buttons | `implementation 'com.google.android.things.contrib:driver-cap12xx:1.0'` | Superseded by [driver-cap1xxx](cap1xxx)

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
