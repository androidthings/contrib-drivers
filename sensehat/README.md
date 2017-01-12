Sense HAT driver for Android Things
=====================================

This driver provides easy access to the peripherals available on the Raspberry Pi [Sense HAT][product]:

- 8×8 RGB LED matrix
- 5-button miniature joystick
- ST LPS25H barometric pressure and temperature sensor
- ST HTS221 relative humidity and temperature sensor
- TODO: ST LSM9DS1 inertial measurement sensor


NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `sensehat` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-sensehat:<version>'
}
```

### Sample usage

```
// import the SenseHat driver
import com.google.android.things.driver.sensehat.SenseHat;
```
```
// Color the LED matrix.
LedMatrix display = SenseHat.openDisplay();
display.draw(Color.MAGENTA);
```
```
//  Display a drawable on the LED matrix.
display.draw(context.getDrawable(android.R.drawable.ic_dialog_alert));
```
```
// Display a gradient on the LED matrix.
Bitmap bitmap = Bitmap.createBitmap(SenseHat.DISPLAY_WIDTH, SenseHat.DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
Canvas canvas = new Canvas(bitmap);
Paint paint = new Paint();
paint.setShader(new RadialGradient(4, 4, 4, Color.RED, Color.BLUE, Shader.TileMode.CLAMP));
canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
display.draw(bitmap);
```
```
// Close the display when done.
display.close();
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

[product]: https://www.raspberrypi.org/products/sense-hat/
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-sensehat
