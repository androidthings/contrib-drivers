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

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.Arrays;

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
    private static final int SPI_BPW = 8; // Bits per word
    private static final int SPI_FREQUENCY = 1000000;
    private static final int SPI_MODE_DEFAULT = SpiDevice.MODE2;

    // Protocol constants for APA102c
    // Start frame: 0x00000000
    private static final int APA_START_FRAME_PACKET_LENGTH = 4;
    // Color frame: 0xe{brightness}{color[0]}{color[1]}{color[2]}
    private static final int APA_COLOR_PACKET_LENGTH = 4;
    // Reset frame: 0x00000000 (for SK9822 variant)
    // See: https://cpldcpu.com/2016/12/13/sk9822-a-clone-of-the-apa102/
    private static final int APA_RESET_FRAME_PACKET_LENGTH = 4;
    // End frame: 0x00000000 (up to 64 LEDs)
    private static final int APA_END_FRAME_PACKET_LENGTH = 4;

    private static final byte APA_START_DATA_BYTE = (byte) 0x00;
    private static final byte APA_RESET_DATA_BYTE = (byte) 0x00;
    private static final byte APA_END_DATA_BYTE = (byte) 0x00;

    // For peripherals access
    private SpiDevice mDevice = null;

    // For composing data to send to the peripheral
    private byte[] mLedData;

    /**
     * Create a new Apa102 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode The {@link Mode} indicating the red/green/blue byte ordering for the device.
     */
    public Apa102(String spiBusPort, Mode ledMode) throws IOException {
        this(spiBusPort, ledMode, Direction.NORMAL, SPI_MODE_DEFAULT);
    }

    /**
     * Create a new Apa102 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode The {@link Mode} indicating the red/green/blue byte ordering for the device.
     * @param direction The {@link Direction} or the led strip.
     */
    public Apa102(String spiBusPort, Mode ledMode, Direction direction) throws IOException {
        this(spiBusPort, ledMode, direction, SPI_MODE_DEFAULT);
    }

    /**
     * Create a new Apa102 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param ledMode The {@link Mode} indicating the red/green/blue byte ordering for the device.
     * @param direction The {@link Direction} or the led strip.
     * @param spiMode the SPI device mode for the bus. Default is MODE2
     */
    public Apa102(String spiBusPort, Mode ledMode, Direction direction, int spiMode)
            throws IOException {
        mLedMode = ledMode;
        mDirection = direction;
        PeripheralManager pioService = PeripheralManager.getInstance();
        mDevice = pioService.openSpiDevice(spiBusPort);
        try {
            configure(mDevice, spiMode);
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
        configure(mDevice, SPI_MODE_DEFAULT);
    }

    private void configure(SpiDevice device, int spiMode) throws IOException {
        // Note: You may need to set bit justification for your board.
        // mDevice.setBitJustification(SPI_BITJUST);
        device.setFrequency(SPI_FREQUENCY);
        device.setMode(spiMode);
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
     * Sets the direction of the LED strip.
     * @param direction The direction of the LED strip, corresponding to {@link Direction}.
     */
    public void setDirection(Direction direction) {
        mDirection = direction;
    }

    /**
     * Get the current {@link Direction}
     */
    public Direction getDirection() {
        return mDirection;
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

        final int size = APA_START_FRAME_PACKET_LENGTH
                + APA_COLOR_PACKET_LENGTH * colors.length
                + APA_RESET_FRAME_PACKET_LENGTH
                + APA_END_FRAME_PACKET_LENGTH;

        int pos = 0;

        if (mLedData == null || mLedData.length < size) {
            mLedData = new byte[size];
            // Add start frame.
            Arrays.fill(mLedData, 0, APA_START_FRAME_PACKET_LENGTH, APA_START_DATA_BYTE);
        }
        pos += APA_START_FRAME_PACKET_LENGTH;

        // Compute the packets to send.
        byte brightness = (byte) (0xE0 | mLedBrightness); // Less brightness possible
        final Direction currentDirection = mDirection; // Avoids reading changes of mDirection during loop
        for (int i = 0; i < colors.length; i++) {
            int di = currentDirection == Direction.NORMAL ? i : colors.length - i - 1;
            copyApaColorData(brightness, colors[di], mLedMode, mLedData, pos);
            pos += APA_COLOR_PACKET_LENGTH;
        }

        // Add reset frame.
        Arrays.fill(mLedData, pos, pos + APA_RESET_FRAME_PACKET_LENGTH, APA_RESET_DATA_BYTE);
        pos += APA_RESET_FRAME_PACKET_LENGTH;
        // Add end frame.
        Arrays.fill(mLedData, pos, pos + APA_END_FRAME_PACKET_LENGTH, APA_END_DATA_BYTE);
        pos += APA_END_FRAME_PACKET_LENGTH;
        if (pos != size) {
            throw new IllegalStateException("end position: " + pos + " should match size: " + size);
        }
        // Write frames to device.
        mDevice.write(mLedData, size);
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
     * Copy the brightness and color values in the form of an APA data packet to the destination
     * array, starting at the specified position.
     */
    @VisibleForTesting
    static void copyApaColorData(byte brightness, int color, Mode ledMode, byte[] dest, int pos) {
        if (dest == null || dest.length < pos + 4) {
            throw new IllegalArgumentException("Destination length must be at least " + (pos + 4));
        }

        dest[pos] = brightness;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        switch(ledMode) {
            case RBG:
                dest[++pos] = (byte) r; dest[++pos] = (byte) b; dest[++pos] = (byte) g;
                break;
            case BGR:
                dest[++pos] = (byte) b; dest[++pos] = (byte) g; dest[++pos] = (byte) r;
                break;
            case BRG:
                dest[++pos] = (byte) b; dest[++pos] = (byte) r; dest[++pos] = (byte) g;
                break;
            case GRB:
                dest[++pos] = (byte) g; dest[++pos] = (byte) r; dest[++pos] = (byte) b;
                break;
            case GBR:
                dest[++pos] = (byte) g; dest[++pos] = (byte) b; dest[++pos] = (byte) r;
                break;
            default:
                // RGB
                dest[++pos] = (byte) r; dest[++pos] = (byte) g; dest[++pos] = (byte) b;
                break;
        }
    }
}
