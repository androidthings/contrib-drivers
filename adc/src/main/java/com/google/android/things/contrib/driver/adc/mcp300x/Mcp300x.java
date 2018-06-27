/*
 * Copyright 2018 Google Inc.
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

package com.google.android.things.contrib.driver.adc.mcp300x;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the MCP300x family of 10-bit analog-digital converters (ADC).
 *
 * <p>Includes MCP3002, MCP3004, MCP3008.
 */
@SuppressWarnings("WeakerAccess")
public class Mcp300x implements AutoCloseable {

    /**
     * Definition of specific ADC chip properties
     * that are not common across the entire device family.
     */
    public enum Configuration {
        MCP3002(2),
        MCP3004(4),
        MCP3008(8);

        final int channelCount;

        Configuration(int channelCount) {
            this.channelCount= channelCount;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_DIFF_0P_1N, MODE_DIFF_1P_0N, MODE_DIFF_2P_3N, MODE_DIFF_3P_2N,
            MODE_DIFF_4P_5N, MODE_DIFF_5P_4N, MODE_DIFF_6P_7N, MODE_DIFF_7P_6N})
    public @interface DifferentialMode {}
    /**
     * Differential input mode between channels 0/1.
     * Channel 0 as IN+.
     */
    public static final int MODE_DIFF_0P_1N = 0x00;
    /**
     * Differential input mode between channels 0/1.
     * Channel 1 as IN+.
     */
    public static final int MODE_DIFF_1P_0N = 0x10;
    /**
     * Differential input mode between channels 2/3.
     * Channel 2 as IN+.
     */
    public static final int MODE_DIFF_2P_3N = 0x20;
    /**
     * Differential input mode between channels 2/3.
     * Channel 3 as IN+.
     */
    public static final int MODE_DIFF_3P_2N = 0x30;
    /**
     * Differential input mode between channels 4/5.
     * Channel 4 as IN+.
     */
    public static final int MODE_DIFF_4P_5N = 0x40;
    /**
     * Differential input mode between channels 4/5.
     * Channel 5 as IN+.
     */
    public static final int MODE_DIFF_5P_4N = 0x50;
    /**
     * Differential input mode between channels 6/7.
     * Channel 6 as IN+.
     */
    public static final int MODE_DIFF_6P_7N = 0x60;
    /**
     * Differential input mode between channels 6/7.
     * Channel 7 as IN+.
     */
    public static final int MODE_DIFF_7P_6N = 0x70;

    private static final byte MODE_SINGLE_ENDED = (byte) 0x80;
    private static final byte MODE_DIFFERENTIAL = (byte) 0x00;

    private SpiDevice mDevice;
    private final Configuration mChipConfiguration;

    /**
     * Create a new Mcp300x interface.
     * @param spiName SPI port name where the device is attached. Cannot be null.
     * @throws IOException If the SpiDevice fails to open.
     */
    public Mcp300x(String spiName, Configuration chip) throws IOException {
        PeripheralManager manager = PeripheralManager.getInstance();
        SpiDevice device = manager.openSpiDevice(spiName);
        mChipConfiguration = chip;
        init(device);
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ Mcp300x(SpiDevice spiDevice, Configuration chip) throws IOException {
        mChipConfiguration = chip;
        init(spiDevice);
    }

    private void init(SpiDevice spiDevice) throws IOException {
        if (spiDevice == null) {
            throw new IllegalArgumentException("Must provide a valid SPI device");
        }

        mDevice = spiDevice;
        mDevice.setMode(SpiDevice.MODE0);
        mDevice.setFrequency(1000000);
    }

    /**
     * Perform a single-ended read on the requested input channel.
     * @param channel Input channel to read
     * @return ADC reading between 0 (Vss) and 1023 (Vref)
     */
    public int readSingleEndedInput(int channel) throws IOException {
        if (channel >= mChipConfiguration.channelCount) {
            throw new IllegalArgumentException("Channel cannot be greater than "
                    + mChipConfiguration.channelCount);
        }

        byte config = MODE_SINGLE_ENDED;
        config |= ((channel & 0x7) << 4);
        byte[] command = {
                0x01, // Start bit
                config, // Configuration bits
                0x00  // Empty clock byte
        };

        byte[] response = new byte[3];
        mDevice.transfer(command, response, 3);
        // Assemble the 10-bit result
        return ((response[1] & 0x3) << 8) | (response[2] & 0xFF);
    }

    /**
     * Perform a pseudo-differential read using the requested mode.
     * The MCP3xxx only supports positive differential reads.
     * Input differences resulting in a negative value return 0.
     * @param mode one of {@link #MODE_DIFF_0P_1N}, {@link #MODE_DIFF_1P_0N},
     *             {@link #MODE_DIFF_2P_3N}, {@link #MODE_DIFF_3P_2N},
     *             {@link #MODE_DIFF_4P_5N}, {@link #MODE_DIFF_5P_4N},
     *             {@link #MODE_DIFF_6P_7N}, or {@link #MODE_DIFF_7P_6N}.
     * @return ADC reading between 0 (Vss) and 1023 (Vref)
     */
    public int readDifferentialInput(@DifferentialMode int mode) throws IOException {
        byte config = MODE_DIFFERENTIAL;
        config |= mode;
        byte[] command = {
                0x01, // Start bit
                config, // Configuration bits
                0x00  // Empty clock byte
        };

        byte[] response = new byte[3];
        mDevice.transfer(command, response, 3);
        // Assemble the 10-bit result
        return ((response[1] & 0x3) << 8) | (response[2] & 0xFF);
    }

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
