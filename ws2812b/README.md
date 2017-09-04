# readme-svg-test
How does it work
---------------------
The WS2812B LED controller needs 24 bits of data (8 bit per color channel) to set the color of one LED. Every further LED of a strip needs another 24 bit long data block. The transmission of these bits is done by sending a chain of high and low voltage pulses to the data line of the LED controller. 
Each separate bit is hereby defined by a high voltage pulse which is followed by a low voltage pulse. Both pulses must have an exact specified duration, so that they are recognized as 1 or 0 bit: 

 <img src="https://rawgit.com/Ic-ks/contrib-drivers/master/ws2812b/ws2812b-timings.svg" width="100%" height="200">

* A 1 bit is defined by a high voltage pulse with a duration of 850 ns which is followed by a low voltage pulse of 400 ns
* A 0 bit is defined by a high voltage pulse with a duration of 400 ns which is followed by a low voltage pulse of 850 ns
* Each pulse can have a deviation of +/- 150 ns 

At the moment there is no direct solution to send such short timed pulses with **different durations** by the API of Android Things. There is however the [Serial Peripheral Interface (SPI)](https://developer.android.com/things/sdk/pio/spi.html) which can send bits as pulses with the **exact same** duration: 
* This duration is indirectly defined by the frequency of the SPI. 
* A transmitted 1 bit results in a short high voltage pulse at the SPI MOSI (Master Out Slave In) pinout 
* A transmitted 0 bit results in a short low voltage pulse at the MOSI pinout

Now the solution gets within reach. To control WS2812B LEDs by the SPI we must find an assembly of SPI bits (bit pattern) and a frequency so that this bit pattern is recognized as one WS2812B bit.
This approach is using an assembly of 3 bits to represent 1 WS2812B bit:

<img src="https://rawgit.com/Ic-ks/contrib-drivers/master/ws2812b/ws2812b-bit-pattern.svg" width="100%" height="200">

The deviation from the WS2812B specified pulse duration is -16 or rather +17 nanoseconds which is within the allowed range of +/-150ns. You could create also a more accurate bit patterns which consists of more than 3 bits. But the more bits you use to express one WS2812b bit, the less is the number of controllable LEDs. Because the SPI is using a fixed sized byte buffer to send the data.

If the the SPI sends more than 8 bits in a row, a pause bit is automatically sent after the 8th bit. This pause bit can be understood as 0 bit between the 8th and the 9th bit. What implications does that have for our bit pattern conversion? Fortunately, this results only in a removing of the 9th bit, because every 9th bit is a 0 bit after we converted a arbitrary bit sequence. For example, if we take the following source bit sequence: 001 and converts it with our bit patterns it will result in: 100 100 11~~0.~~ As you can see the 9th bit is a 0 bit and can be removed because it will be sent automatically by the SPI device. This 9th 0 bit exists for any possible 3 bit long source sequence after the conversation:

| Source bit sequence | Destination bit sequence | 
| ------------------- |:------------------------:| 
| 111                 | 110 110 11~~0~~          |
| 011                 | 100 110 11~~0~~          |
| 001                 | 100 100 11~~0~~          |
| 000                 | 100 100 10~~0~~          |
| 010                 | 100 110 10~~0~~          |
| 100                 | 110 100 10~~0~~          |
| 110                 | 110 110 10~~0~~          |

With this in mind any possible 24 bit color can be represented by 8 times 8 converted bits. Which means that 8 bytes must be sent to set the color of 1 LED. 

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
