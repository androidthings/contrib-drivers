# readme-svg-test
How does it work
---------------------
The WS2812B LED controller needs 24 bits of data (8 bit per color channel) to set the color of one LED. Every further LED of a strip needs another 24 bit long block of data. The transmission of these bits is done by sending a chain of high and low voltage pulses over the data line of the LED controller. 
Each separate bit is hereby defined by a high voltage pulse which is followed by a low voltage pulse. The recognition as 0 or 1 bit is defined by the timings of these pulses:

 <img src="https://rawgit.com/Ic-ks/contrib-drivers/master/ws2812b/ws2812b-timings.svg" width="100%" height="200">

* A 0 bit is defined by a high voltage pulse with a duration of 400 ns which is followed by a low voltage pulse of 850 ns
* A 1 bit is defined by a high voltage pulse with a duration of 850 ns which is followed by a low voltage pulse of 400 ns
* Each pulse can have a deviation of +/- 150 ns 

At the moment there is no direct solution to send such short timed pulses with **different durations** by the API of Android Things. There is however the [Serial Peripheral Interface (SPI)](https://developer.android.com/things/sdk/pio/spi.html) which can send bits as voltage pulses. Whereas each single bit results in a pulse of the **exact same** duration. 

* This duration is indirectly defined by the frequency of the SPI. 
* A transmitted 1 bit results in a short high voltage pulse at the SPI MOSI (Master Out Slave In) pinout 
* A transmitted 0 bit results in a short low voltage pulse at the MOSI pinout

Now the solution gets within reach. To control WS2812B LEDs by the SPI, we must find two assemblies of bits (hereinafter bit pattern) and a frequency so that each of these bit patterns results in a sequence of voltage pulses which are recognized as 0 or 1 bit by the receiving WS2812B controller. Based on these bit patterns we are able to convert any color data to a sequence of bits which are recognizable by the WS2812B controller when it is sent by SPI. This library is using a 3 bit sized bit pattern to convert one input bit:

<img src="https://rawgit.com/Ic-ks/contrib-drivers/master/ws2812b/ws2812b-bit-pattern.svg" width="100%" height="200">

The deviation from the WS2812B specified pulse duration is -16 or rather +17 nanoseconds which is within the allowed range of +/-150ns. It is possible to create a more accurate bit pattern with more than 3 bits, but the greater size of the bit pattern, the faster is the fixed size SPI buffer full and the less is the number of controllable LEDs.

The last problem we must solve, is the low voltage pause between each transmitted byte: If the the SPI sends more than 8 bits in a row, a short break in form of a low voltage pulse is done automatically. This pause pulse has the same duration as the bits, which are really sent. So it can be understood as a automatically inserted 0 bit between the 8th and the 9th bit. To keep our data safe from corruption we must handle this "inserted" bit. Fortunately, any arbitrary sequence of our described bit patterns are resulting in a row of bits where every 9th bit is a 0 bit. So a simple solution is the removing of this last bit like shown in the following table:

| Source bit sequence | Destination bit sequence | 
| ------------------- |:------------------------:| 
| 111                 | 110 110 11~~0~~          |
| 011                 | 100 110 11~~0~~          |
| 001                 | 100 100 11~~0~~          |
| 000                 | 100 100 10~~0~~          |
| 010                 | 100 110 10~~0~~          |
| 100                 | 110 100 10~~0~~          |
| 110                 | 110 110 10~~0~~          |

With this in mind we can represent 3 source bits by 1 destination byte. So any possible 24 bit color needs to be converted to 8 byte sized sequence of bit patterns.

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
