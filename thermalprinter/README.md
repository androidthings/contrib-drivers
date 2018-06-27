# Thermal Library driver for Android Things

This driver exposes a [UART](https://developer.android.com/things/reference/com/google/android/things/pio/UartDevice.html)
interface to a serial thermal printer. This printer can print text with a variety of text styles. Additionally, it
can print bitmaps and barcodes.

This driver works with the thermal printers from Adafruit, such as the
[CsnA2 Mini Thermal Printer](https://www.adafruit.com/product/597).
When you power on your device, press the button in order to get a printout of device information. Take
note of the firmware version. This driver supports firmware v2.68 and above.

The thermal printer requires an external power supply. It cannot be
powered by the developer board.


At this moment the driver has only been tested on the Raspberry Pi 3 and
the imx7.d.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.


## How to use the driver

### Gradle dependency

To use the `thermalprinter` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-thermalprinter:<version>'
}
```

### Sample usage


```java
// import the Thermal Printer driver
import com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter;
```

```java
// Start thermal printer.
mThermalPrinter = new ThermalPrinter("UART0");

// Make sure to call mThermalPrinter.close() in the onDestroy() method in your activity
```

```java
// Start printing text.
mThermalPrinter.enqueue(new TextJob().printText("Hello World"));
```

```java
// Print stylized text.
mThermalPrinter.enqueue(new TextJob()
    .justify(CsnA2.JUSTIFY_CENTER)
    .setDoubleHeight(true)
    .printText("Hello World"));
```

```java
// Print a barcode.
mThermalPrinter.enqueue(new BarcodeJob()
    .setBarcodeHeight(60)
    .printBarcode("GOOGLE", CsnA2.UPC_A));
```

```java
// Print a bitmap
mThermalPrinter.enqueue(new BitmapJob().printBitmap(bitmap));
// Feed several lines afterward
mThermalPrinter.enqueue(PrinterJob.feedLines(3));
```

```java
// Close Thermal Printer driver
try {
  mThermalPrinter.close();
} catch (IOException e) {
  Log.e(TAG, "Unable to close thermal printer driver", e);
} finally {
  mThermalPrinter = null;
}
```

## License

Copyright 2018 Google Inc.

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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-thermalprinter