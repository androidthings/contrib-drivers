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

    public enum Direction {
        NORMAL,
        REVERSED,
    }

    // RGB LED strip configuration that must be provided by the caller.
    private Mode mLedMode;

    // RGB LED strip settings that have sensible defaults.
    private byte mLedBrightness = (byte) (0xE0 | 12); // 0 ... 31

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
     * @param ledBrightness The brightness of the LED strip (0 ... 31).
     */
    public void setBrightness(int ledBrightness) {
        if (ledBrightness < 0 || ledBrightness > 31) {
            throw new IllegalArgumentException("Brightness needs to be between 0 and 31");
        }
        mLedBrightness = (byte) (0xE0 | ledBrightness); // Less brightness possible
    }

    /**
     * Writes the current RGB Led data to the peripheral bus.
     * @param colors An array of integers corresponding to a {@link Color}.
     * @throws IOException
     */
    public void write(int[] colors) throws IOException {
        byte[] ledData = new byte[(APA102_PACKET_LENGTH * (2 + colors.length))];

        // Add the RGB LED start bits (0 ... 0)
        System.arraycopy(APA_START_DATA, 0, ledData, 0, APA102_PACKET_LENGTH);

        // Compute the packets to send.
        for (int i = 0; i < colors.length; i++) {
            int position = ((i + 1) * APA102_PACKET_LENGTH);
            int di = mDirection == Direction.NORMAL ? i : colors.length - i - 1;
            System.arraycopy(getApaColorData(colors[di]), 0, ledData, position,
                    APA102_PACKET_LENGTH);
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
    private byte[] getApaColorData(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        switch(mLedMode) {
            case RBG:
                return new byte[] {mLedBrightness, (byte) r, (byte) b, (byte) g};
            case BGR:
                return new byte[] {mLedBrightness, (byte) b, (byte) g, (byte) r};
            case BRG:
                return new byte[] {mLedBrightness, (byte) b, (byte) r, (byte) g};
            case GRB:
                return new byte[] {mLedBrightness, (byte) g, (byte) r, (byte) b};
            case GBR:
                return new byte[] {mLedBrightness, (byte) g, (byte) b, (byte) r};
            default:
                // RGB
                return new byte[] {mLedBrightness, (byte) r, (byte) g, (byte) b};
        }
    }
}
