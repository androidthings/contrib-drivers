# readme-svg-test
How does it work
---------------------
The WS2812B LED controller needs 24 bits of data (8 bit per color channel) to set the color of one LED. Every further LED of a strip needs another 24 bit long data block. The transmission of these bits is done by sending a chain of high and low voltage pulses to the data line of the LED controller. 
Each separate bit is hereby defined by a high voltage pulse which is followed by a low voltage pulse. Both pulses must have an exact specified duration, so that they are recognized as 1 or 0 bit: 

 <img src="https://rawgit.com/Ic-ks/readme-svg-test/master/ws2812b-timings.svg" width="100%" height="200">

* A 1 bit is defined by a high voltage pulse with a duration of 850 ns which is followed by a low voltage pulse of 400 ns
* A 0 bit is defined by a high voltage pulse with a duration of 400 ns which is followed by a low voltage pulse of 850 ns
* Each pulse can have a deviation of +/- 150 ns 

At the moment there is no direct solution to send such short timed pulses with **different durations** by the API of Android Things. There is however the [Serial Peripheral Interface (SPI)](https://developer.android.com/things/sdk/pio/spi.html) which is only able to send short timed pulses with the **exact same** duration: 
* This duration is indirectly defined by the frequency of the SPI. 
* A transmitted 1 bit results in a short high voltage pulse at the SPI MOSI (Master Out Slave In) pinout 
* A transmitted 0 bit results in a short low voltage pulse at the MOSI pinout

Now the solution gets within reach. To control WS2812B LEDs by the SPI we must find an assembly of SPI bits (bit pattern) and a frequency so that this bit pattern is recognized as one WS2812B bit.
This approach is using an assembly of 3 bits to represent 1 WS2812B bit:

<img src="https://rawgit.com/Ic-ks/readme-svg-test/master/ws2812b-bit-pattern.svg" width="100%" height="200">

The deviation from the WS2812B specified pulse duration is -16 or rather +17 nanoseconds which is within the allowed range of +/-150ns. You could create also a more accurate bit patterns which consists of more than 3 bits. But the more bits you use to express one WS2812b bit, the less is the number of controllable LEDs. Because the SPI is using a fixed sized byte buffer to send the data.    
