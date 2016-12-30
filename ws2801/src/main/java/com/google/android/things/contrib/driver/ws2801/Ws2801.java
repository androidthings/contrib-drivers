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

package com.google.android.things.contrib.driver.ws2801;

import android.graphics.Color;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

/**
 * Device driver for WS2801 RGB LEDs using 2-wire SPI.
 * <p>
 * For more information on SPI, see:
 * https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus
 * For information on the WS2801 protocol, see:
 * https://cdn-shop.adafruit.com/datasheets/WS2801.pdf
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class Ws2801 implements AutoCloseable {
    private static final String TAG = "Ws2801";

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

    // Direction of the led strip;
    private Direction mDirection;

    // Device SPI Configuration constants
    private static final int WS2801_PACKET_LENGTH = 3;
    private static final int SPI_BPW = 8; // Bits per word
    private static final int SPI_FREQUENCY = 1000000;
    private static final int SPI_MODE = SpiDevice.MODE0; // Mode 0 seems to work best for WS2801

    // For peripherals access
    private SpiDevice mDevice = null;

    /**
     * Create a new Ws2801 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode    The {@link Mode} indicating the red/green/blue byte ordering for the device.
     */
    public Ws2801(String spiBusPort, Mode ledMode) throws IOException {
        this(spiBusPort, ledMode, Direction.NORMAL);
    }

    /**
     * Create a new Ws2801 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode    The {@link Mode} indicating the red/green/blue byte ordering for the device.
     * @param direction  The {@link Direction} or the led strip.
     */
    public Ws2801(String spiBusPort, Mode ledMode, Direction direction) throws IOException {
        mLedMode = ledMode;
        mDirection = direction;
        PeripheralManagerService pioService = new PeripheralManagerService();
        mDevice = pioService.openSpiDevice(spiBusPort);
        try {
            configure(mDevice);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new Ws2801 driver.
     *
     * @param device  {@link SpiDevice} where the LED strip is attached to.
     * @param ledMode The {@link Mode} indicating the red/green/blue byte ordering for the device.
     */
    /*package*/ Ws2801(SpiDevice device, Mode ledMode, Direction direction) throws IOException {
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
     * Writes the current RGB Led data to the peripheral bus.
     *
     * @param colors An array of integers corresponding to a {@link Color}.
     * @throws IOException
     */
    public void write(int[] colors) throws IOException {
        byte[] ledData = new byte[WS2801_PACKET_LENGTH * colors.length];

        // Compute the packets to send.
        for (int i = 0; i < colors.length; i++) {
            int outputPosition = i * WS2801_PACKET_LENGTH;
            int di = mDirection == Direction.NORMAL ? i : colors.length - i - 1;
            System.arraycopy(getWsColorData(colors[di]), 0, ledData, outputPosition, WS2801_PACKET_LENGTH);
        }

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
     * Returns an WS2801 packet corresponding to the current brightness and given {@link Color}.
     *
     * @param color The {@link Color} to retrieve the protocol packet for.
     * @return WS2801 packet corresponding to the current brightness and given {@link Color}.
     */
    private byte[] getWsColorData(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        switch (mLedMode) {
            case RBG:
                return new byte[]{(byte) r, (byte) b, (byte) g};
            case BGR:
                return new byte[]{(byte) b, (byte) g, (byte) r};
            case BRG:
                return new byte[]{(byte) b, (byte) r, (byte) g};
            case GRB:
                return new byte[]{(byte) g, (byte) r, (byte) b};
            case GBR:
                return new byte[]{(byte) g, (byte) b, (byte) r};
            default:
                // RGB
                return new byte[]{(byte) r, (byte) g, (byte) b};
        }
    }
}
