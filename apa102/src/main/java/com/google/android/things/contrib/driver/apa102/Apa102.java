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

package com.google.android.things.contrib.driver.apa102;

import android.graphics.Color;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

/**
 * Device driver for APA102 / Dotstar RGB LEDs using 2-wire SPI.
 *
 * For more information on SPI, see:
 *   https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus
 * For information on the APA102 protocol, see:
 *   https://cpldcpu.wordpress.com/2014/11/30/understanding-the-apa102-superled
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class Apa102 implements AutoCloseable {
    private static final String TAG = "Apa102";

    /**
     * Color ordering for the RGB LED messages; the most common modes are BGR and RGB.
     */
    public enum Mode {
        RGB,
        RBG,
        GRB,
        GBR,
        BRG,
        BGR
    }

    /**
     * The direction to apply colors when writing LED data
     */
    public enum Direction {
        NORMAL,
        REVERSED,
    }

    /**
     * The maximum brightness level
     */
    public static final int MAX_BRIGHTNESS = 31;

    // RGB LED strip configuration that must be provided by the caller.
    private Mode mLedMode;

    // RGB LED strip settings that have sensible defaults.
    private int mLedBrightness = MAX_BRIGHTNESS >> 1; // default to half

    // Direction of the led strip;
    private Direction mDirection;

    // Device SPI Configuration constants
    private static final int APA102_PACKET_LENGTH = 4;
    private static final int SPI_BPW = 8; // Bits per word
    private static final int SPI_FREQUENCY = 1000000;
    private static final int SPI_MODE = 2;

    // Protocol constants for APA102c
    private static final byte[] APA_START_DATA = {0, 0, 0, 0};
    private static final byte[] APA_END_DATA = {-1, -1, -1, -1};

    // For peripherals access
    private SpiDevice mDevice = null;

    /**
     * Create a new Apa102 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode The {@link Mode} indicating the red/green/blue byte ordering for the device.
     */
    public Apa102(String spiBusPort, Mode ledMode) throws IOException {
        this(spiBusPort, ledMode, Direction.NORMAL);
    }

    /**
     * Create a new Apa102 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode The {@link Mode} indicating the red/green/blue byte ordering for the device.
     * @param direction The {@link Direction} or the led strip.
     */
    public Apa102(String spiBusPort, Mode ledMode, Direction direction) throws IOException {
        mLedMode = ledMode;
        mDirection = direction;
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
     * @param ledMode The {@link Mode} indicating the red/green/blue byte ordering for the device.
     */
    @VisibleForTesting
    /*package*/ Apa102(SpiDevice device, Mode ledMode, Direction direction) throws IOException {
        mLedMode = ledMode;
        mDirection = direction;
        mDevice = device;
        configure(mDevice);
    }

    private void configure(SpiDevice device) throws IOException {
        // Note: You may need to set bit justification for your board.
        // mDevice.setBitJustification(SPI_BITJUST);
        device.setFrequency(SPI_FREQUENCY);
        device.setMode(SPI_MODE);
        device.setBitsPerWord(SPI_BPW);
    }

    /**
     * Sets the brightness for all LEDs in the strip.
     * @param ledBrightness The brightness of the LED strip, between 0 and {@link #MAX_BRIGHTNESS}.
     */
    public void setBrightness(int ledBrightness) {
        if (ledBrightness < 0 || ledBrightness > MAX_BRIGHTNESS) {
            throw new IllegalArgumentException("Brightness needs to be between 0 and "
                    + MAX_BRIGHTNESS);
        }
        mLedBrightness = ledBrightness;
    }

    /**
     * Get the current brightness level
     */
    public int getBrightness() {
        return mLedBrightness;
    }

    /**
     * Writes the current RGB Led data to the peripheral bus.
     * @param colors An array of integers corresponding to a {@link Color}.
     * @throws IOException
     */
    public void write(int[] colors) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("SPI device not open");
        }

        byte[] ledData = new byte[(APA102_PACKET_LENGTH * (2 + colors.length))];

        // Add the RGB LED start bits (0 ... 0)
        System.arraycopy(APA_START_DATA, 0, ledData, 0, APA102_PACKET_LENGTH);

        // Compute the packets to send.
        byte brightness = (byte) (0xE0 | mLedBrightness); // Less brightness possible
        for (int i = 0; i < colors.length; i++) {
            int position = ((i + 1) * APA102_PACKET_LENGTH);
            int di = mDirection == Direction.NORMAL ? i : colors.length - i - 1;
            byte[] colorData = getApaColorData(colors[di], brightness, mLedMode);
            System.arraycopy(colorData, 0, ledData, position, APA102_PACKET_LENGTH);
        }

        // Add the RGB LED end bits
        System.arraycopy(APA_END_DATA, 0, ledData, ledData.length - 4, 4);

        mDevice.write(ledData, ledData.length);
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

    /**
     * Returns an APA102 packet corresponding to the current brightness and given {@link Color}.
     * @param color The {@link Color} to retrieve the protocol packet for.
     * @return APA102 packet corresponding to the current brightness and given {@link Color}.
     */
    @VisibleForTesting
    static byte[] getApaColorData(int color, byte brightness, Mode ledMode) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        switch(ledMode) {
            case RBG:
                return new byte[] {brightness, (byte) r, (byte) b, (byte) g};
            case BGR:
                return new byte[] {brightness, (byte) b, (byte) g, (byte) r};
            case BRG:
                return new byte[] {brightness, (byte) b, (byte) r, (byte) g};
            case GRB:
                return new byte[] {brightness, (byte) g, (byte) r, (byte) b};
            case GBR:
                return new byte[] {brightness, (byte) g, (byte) b, (byte) r};
            default:
                // RGB
                return new byte[] {brightness, (byte) r, (byte) g, (byte) b};
        }
    }
}
