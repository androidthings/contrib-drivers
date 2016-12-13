Android Things user-space drivers
=================================

Sample peripheral drivers for Android Things.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.


How to use a driver
===================

For your convenience, drivers in this repository are also published to JCenter
as Maven artifacts. Look at their artifact and group ID in their build.gradle
and add them as dependencies to your own project.

For example, to use the `button` driver, version `0.1`, simply add the line
below to your project's `build.gradle`:


```
dependencies {
    compile 'com.google.android.things.contrib:driver-button:0.1'
}
```


Current contrib drivers
-----------------------

Driver | Type | Usage (add to your gradle dependencies) | Sample code
:---:|:---:| --- | ---
[driver-apa102](apa102) | RGB LED strip |`compile 'com.google.android.things.contrib:driver-apa102:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-apa102/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/apa102)
[driver-bmx280](bmx280) | temperature and pressure sensor |`compile 'com.google.android.things.contrib:driver-bmx280:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-bmx280/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/bmx280)
[driver-button](button) | push button over GPIO |`compile 'com.google.android.things.contrib:driver-button:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-button/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/button)
[driver-cap12xx](cap12xx) | capacitive touch buttons |`compile 'com.google.android.things.contrib:driver-cap12xx:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-cap12xx/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/cap12xx)
[driver-gps](gps) | GPS |`compile 'com.google.android.things.contrib:driver-gps:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-gps/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/gps)
[driver-ht16k33](ht16k33) | 7-digit alphanumeric segment display |`compile 'com.google.android.things.contrib:driver-ht16k33:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-ht16k33/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/ht16k33)
[driver-mma7660fc](mma7660fc) | accelerometer sensor |`compile 'com.google.android.things.contrib:driver-mma7660fc:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-mma7660fc/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/mma7660fc)
[driver-pwmservo](pwmservo) | PWM servo |`compile 'com.google.android.things.contrib:driver-pwmservo:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-pwmservo/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/pwmservo)
[driver-pwmspeaker](pwmspeaker) | PWM speaker/buzzer |`compile 'com.google.android.things.contrib:driver-pwmspeaker:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-pwmspeaker/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/pwmspeaker)
[driver-rainbowhat](rainbowhat) | metadriver for the Rainbow HAT |`compile 'com.google.android.things.contrib:driver-rainbowhat:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-rainbowhat/_latestVersion) |[sample](https://github.com/androidthings/weatherstation)
[driver-ssd1306](ssd1306) | OLED display |`compile 'com.google.android.things.contrib:driver-ssd1306:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-ssd1306/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/ssd1306)
[driver-tm1637](tm1637) | 4-digit numeric segment display |`compile 'com.google.android.things.contrib:driver-tm1637:<version>'`<br>[(see latest version)](https://bintray.com/google/androidthings/contrib-driver-tm1637/_latestVersion) |[sample](https://github.com/androidthings/contrib-drivers/tree/master/tm1637)

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
