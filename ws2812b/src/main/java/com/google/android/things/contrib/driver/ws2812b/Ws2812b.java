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
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.ByteArrayOutputStream;
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

    private final ColorValueBitPatternConverter bitPatternConverter = new ColorValueBitPatternConverter();

    // RGB LED strip configuration that must be provided by the caller.
    @LedMode
    private int mLedMode;

    // For peripherals access
    private SpiDevice mDevice = null;

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
     */
    public Ws2812b(String spiBusPort, @LedMode int ledMode) throws IOException {
        mLedMode = ledMode;
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
     * Create a new Apa102 driver.
     *
     * @param device {@link SpiDevice} where the LED strip is attached to.
     * @param ledMode The {@link LedMode} indicating the red/green/blue byte ordering for the device.
     */
    @VisibleForTesting
    /*package*/ Ws2812b(SpiDevice device, @LedMode int ledMode) throws IOException {
        mLedMode = ledMode;
        mDevice = device;
        configure(mDevice);
    }

    private void configure(SpiDevice device) throws IOException {
        // Note: You may need to set bit justification for your board.
        // mDevice.setBitJustification(SPI_BITJUST);
        device.setFrequency(3225806);
        device.setMode(SpiDevice.MODE1);
        device.setBitsPerWord(8);
    }

    /**
     * Writes the current RGB Led data to the peripheral bus.
     * @param colors An array of integers corresponding to a {@link Color}.
     * @throws IOException
     */
    public void write(@ColorInt int[] colors) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("SPI device not open");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        for (int color : colors)
        {
            writeColorPattern(outputStream, color);
        }

        byte[] colorPattern = outputStream.toByteArray();

        mDevice.write(colorPattern, colorPattern.length);
    }

    private void writeColorPattern(@NonNull ByteArrayOutputStream outputStream, @ColorInt int color) throws IOException
    {
        byte[] red = bitPatternConverter.convertColorValue(Color.red(color));
        byte[] green = bitPatternConverter.convertColorValue(Color.green(color));
        byte[] blue = bitPatternConverter.convertColorValue(Color.blue(color));

        switch (mLedMode)
        {
            case BGR:
                outputStream.write( blue );
                outputStream.write( green );
                outputStream.write( red );
                break;
            case BRG:
                outputStream.write( blue );
                outputStream.write( red );
                outputStream.write( green );
                break;
            case GBR:
                outputStream.write( green );
                outputStream.write( blue );
                outputStream.write( red );
                break;
            case GRB:
                outputStream.write( green );
                outputStream.write( red );
                outputStream.write( blue );
                break;
            case RBG:
                outputStream.write( red );
                outputStream.write( blue );
                outputStream.write( green );
                break;
            case RGB:
                outputStream.write( red );
                outputStream.write( green );
                outputStream.write( blue );
                break;
        }
    }

    /**
     * Releases the SPI interface and related resources.
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
