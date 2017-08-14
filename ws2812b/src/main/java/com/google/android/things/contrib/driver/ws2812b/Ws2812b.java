/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.ws2812b;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Device driver for WS2812B LEDs using SPI.
 *
 * For more information on SPI, see:
 *   https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus
 * For information on the WS2812B protocol, see:
 *   https://cpldcpu.com/2014/01/14/light_ws2812-library-v2-0-part-i-understanding-the-ws2812/
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class Ws2812b implements AutoCloseable {
    private static final String TAG = "Ws2812b";

    /**
     * Color ordering for the RGB LED messages; the most common modes are BGR and RGB.
     */
    @Retention(SOURCE)
    @IntDef({RGB, RBG, GRB, GBR, BRG, BGR})
    public @interface LedMode {}
    public static final int RGB = 0;
    public static final int RBG = 1;
    public static final int GRB = 2;
    public static final int GBR = 3;
    public static final int BRG = 4;
    public static final int BGR = 5;

    // For peripherals access
    private SpiDevice mDevice = null;

    private final ColorToBitPatternConverter mColorToBitPatternConverter;

    /**
     * Create a new WS2812B driver.
     *
     * @param spiBusPort Name of the SPI bus
     */
    public Ws2812b(String spiBusPort) throws IOException {
        this(spiBusPort, GRB);
    }

    /**
     * Create a new WS2812B driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode The {@link LedMode} indicating the red/green/blue byte ordering for the device.
     * @throws IOException if the initialization of the SpiDevice fails
     *
     */
    public Ws2812b(String spiBusPort, @LedMode int ledMode) throws IOException {
        mColorToBitPatternConverter = new ColorToBitPatternConverter(ledMode);
        PeripheralManagerService pioService = new PeripheralManagerService();
        mDevice = pioService.openSpiDevice(spiBusPort);
        try {
            configure(mDevice);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new WS2812B driver.
     *
     * @param device {@link SpiDevice} where the LED strip is attached to.
     * @param ledMode The {@link LedMode} indicating the red/green/blue byte ordering for the device.
     */
    @VisibleForTesting
    /*package*/ Ws2812b(SpiDevice device, @LedMode int ledMode) throws IOException {
        mColorToBitPatternConverter = new ColorToBitPatternConverter(ledMode);
        mDevice = device;
        configure(mDevice);
    }

    private void configure(SpiDevice device) throws IOException {

        double durationOfOneBitInNs = 417.0;
        double durationOfOneBitInS = durationOfOneBitInNs * Math.pow(10, -9);
        int frequencyInHz = (int) Math.round(1.0 / durationOfOneBitInS);

        device.setFrequency(frequencyInHz);
        device.setBitsPerWord(8);
        device.setDelay(0);
    }

    /**
     * Writes the current RGB Led data to the peripheral bus.
     * @param colors An array of integers corresponding to a {@link Color}.
     * @throws IOException if writing to the SPi device fails
     */
    public void write(@ColorInt int[] colors) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("SPI device not opened");
        }

        byte[] convertedColors = mColorToBitPatternConverter.convertColorsToBitPattern(colors);

        mDevice.write(convertedColors, convertedColors.length);
    }

    /**
     * Releases the SPI interface and related resources.
     * @throws IOException if the SpiDevice is already closed
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }
}
