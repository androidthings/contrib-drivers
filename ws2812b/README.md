WS2812B LED driver for Android Things
=====================================

This driver supports WS2812B LEDs (maybe WS1812S and SK6812 run too).

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `WS2812B` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-ws2812b:<version>'
}
```

### Sample usage

```java
import com.google.android.things.contrib.driver.ws2812b.Ws2812b;

// Access the LED strip:

Ws2812b mWs2812b;

try {
    mWs2812b = new Ws2812b(spiBusName);
} catch (IOException e) {
    // couldn't configure the device...
}

// Light it up!

int[] colors = new int[] {Color.RED, Color.GREEN, Color.BLUE};
try {
    mWs2812b.write(colors);
} catch (IOException e) {
    // error setting LEDs
}

// Close the LED strip when finished:

try {
    mWs2812b.close();
} catch (IOException e) {
    // error closing LED strip
}
```

How it works
------------

### The WS2812B data format

The WS2812B LED controller needs 24 bits of data (8 bits per color channel) to set the color of one LED. Every further LED of a strip needs another 24 bit long block of data. The transmission of these bits is done by sending a chain of high and low voltage pulses over the data line of the LED controller. 
Each separate bit is hereby defined by a high voltage pulse which is followed by a low voltage pulse. The recognition as 0- or 1-bit is defined by the timings of these pulses:

<p align="center"> 
<img align="center" src="https://rawgit.com/Ic-ks/contrib-drivers/master/ws2812b/ws2812b-timings.svg"/>
</p>

* The 0-bit is defined by a high voltage pulse with a duration of 400 ns which is followed by a low voltage pulse of 850 ns
* The 1-bit is defined by a high voltage pulse with a duration of 850 ns which is followed by a low voltage pulse of 400 ns
* Each pulse can have a deviation of +/- 150 ns

At the moment there is no way to send such short timed pulses having _different durations_ via the Android Things API.

### The Serial Peripheral Interface

The [Serial Peripheral Interface (SPI)](https://developer.android.com/things/sdk/pio/spi.html) can send bits as voltage pulses. Those pulses all have the same specified duration.

* The pulse duration is indirectly defined by the frequency of the SPI
* A transmitted 1-bit results in a short high voltage pulse at the SPI MOSI (Master Out Slave In) pinout 
* A transmitted 0-bit results in a short low voltage pulse at the MOSI pinout

### Approximating the WS2812B data format using SPI

In order to control WS2812B LEDs via the SPI, we must find two assemblies of bits (hereinafter bit pattern) and a frequency such that each of these bit patterns results in a sequence of voltage pulses which are recognized as 0 or 1 bit by the receiving WS2812B controller. With these two bit patterns we are able to convert every bit of an arbitrary array of color data. If then the converted data is sent to the WS2812B controller by SPI, the controller will recognize the orignal color and light up the LEDs accordingly. A possible solution for this approach are the two 3 bit sized patterns below:

<p align="center"> 
<img align="center" src="https://rawgit.com/Ic-ks/contrib-drivers/master/ws2812b/ws2812b-bit-pattern.svg"/>
</p>

The deviation from the WS2812B specified pulse duration is -16 ns and +17 ns respectively, which is within the allowed range of +/- 150 ns. It is possible to create a more accurate bit pattern with more than 3 bits, but increasing the size of the bit pattern als reduces the number of controllable LEDs as the fixed-size SPI buffer fills up more quickly. The appropriate frequency is defined by the duration of 1 bit (417 ns):

<p align="center"> 
<img align="center" src="http://latex.codecogs.com/gif.latex?f%3D%5Cfrac%7B1%20%7D%7B417%20%5Ccdot%2010%5E%7B-9%7D%7DHz"/>
</p>

### Handling pauses between SPI words

SPI will automatically insert low voltage pauses between transmitted words. This short break marks the end of a transmitted word and has the same duration as a single bit. If left unhandled, those pauses would interfere with our approximated data format and lead to incorrect colors. By considering a word size of 8 bits (maximum size), the pause can be understood as an automatically inserted 0-bit between the 8th and the 9th bit. Fortunately, all possible bit sequences yield a bit pattern where every 9th bit is a 0-bit. Hence an easy solution is to discard the last bit like shown in the table below:

| Source bit sequence | Resulting bit pattern  | Without trailing 0-bit   |
| ------------------- |:----------------------:|:------------------------:|
| 000                 | 100 100 10**0**        | 100 100 10               |
| 001                 | 100 100 11**0**        | 100 100 11               |
| 010                 | 100 110 10**0**        | 100 110 10               |
| 011                 | 100 110 11**0**        | 100 110 11               |
| 100                 | 110 100 10**0**        | 110 100 10               |
| 110                 | 110 110 10**0**        | 110 110 10               |
| 111                 | 110 110 11**0**        | 110 110 11               |

With this in mind we can represent three source bits using one destination byte. So any possible 24 bit color needs to be converted to a 8 byte sized sequence of bit patterns (24 / 3 = 8). 

To prevent excessive memory usage this driver stores and maps only 12 bit numbers to their corresponding 4 byte sized bit patterns ([TwelveBitIntToBitPatternMapper.java](/ws2812b/src/main/java/com/google/android/things/contrib/driver/ws2812b/TwelveBitIntToBitPatternMapper.java)). The full 8 byte sized bit pattern for 24 bits of color data is then constructed by two 4 byte sized bit patterns ([ColorToBitPatternConverter.java](/ws2812b/src/main/java/com/google/android/things/contrib/driver/ws2812b/ColorToBitPatternConverter.java)). Last but not least, most WS2812B controllers expect GRB as order of the incoming color. So a reordering from RGB to the expected order must be done before the bit pattern conversion ([ColorChannelSequence.java](/ws2812b/src/main/java/com/google/android/things/contrib/driver/ws2812b/ColorChannelSequence.java)).

References
----------
* https://wp.josh.com/2014/05/13/ws2812-neopixels-are-not-so-finicky-once-you-get-to-know-them/
* https://cpldcpu.com/2014/01/14/light_ws2812-library-v2-0-part-i-understanding-the-ws2812/
* https://cdn-shop.adafruit.com/datasheets/WS2812B.pdf

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

[jcenter]: https://bintray.com/google/androidthings/contrib-driver-ws2812b/_latestVersion
