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

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

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

    @NonNull
    private final ColorToBitPatternConverter mColorToBitPatternConverter;
    private SpiDevice mDevice = null;

    /**
     * Create a new WS2812B driver.
     *
     * @param spiBusPort Name of the SPI bus
     */
    public Ws2812b(String spiBusPort) throws IOException {
        this(spiBusPort, new ColorToBitPatternConverter(ColorChannelSequence.GRB));
    }

    /**
     * Create a new WS2812B driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param colorChannelSequence The {@link ColorChannelSequence.Sequence} indicates the red/green/blue byte order for the LED strip.
     * @throws IOException if the initialization of the SpiDevice fails
     *
     */
    public Ws2812b(String spiBusPort, @ColorChannelSequence.Sequence int colorChannelSequence) throws IOException {
        this (spiBusPort, new ColorToBitPatternConverter(colorChannelSequence));
    }

    private Ws2812b(String spiBusPort, @NonNull ColorToBitPatternConverter colorToBitPatternConverter) throws IOException {
        mColorToBitPatternConverter = colorToBitPatternConverter;
        mDevice = new PeripheralManagerService().openSpiDevice(spiBusPort);
        try {
            initSpiDevice(mDevice);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    @VisibleForTesting
    /*package*/ Ws2812b(SpiDevice device, @NonNull ColorToBitPatternConverter colorToBitPatternConverter) throws IOException {
        mColorToBitPatternConverter = colorToBitPatternConverter;
        mDevice = device;
        initSpiDevice(mDevice);
    }

    private void initSpiDevice(SpiDevice device) throws IOException {

        double durationOfOneBitInNs = 417.0;
        double durationOfOneBitInS = durationOfOneBitInNs * Math.pow(10, -9);
        int frequencyInHz = (int) Math.round(1.0 / durationOfOneBitInS);

        device.setFrequency(frequencyInHz);
        device.setBitsPerWord(8);
        device.setDelay(0);
    }

    /**
     * Transforms the passed color array and writes it to the SPI connected WS2812b LED strip.
     * @param colors An array of 24 bit RGB color integers {@link ColorInt}
     * @throws IOException if writing to the SPI device fails
     */
    public void write(@NonNull @ColorInt int[] colors) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("SPI device not opened");
        }

        byte[] convertedColors = mColorToBitPatternConverter.convertToBitPattern(colors);
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
