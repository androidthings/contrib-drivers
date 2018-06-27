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

package com.google.android.things.contrib.driver.adc.ads1xxx;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the ADS1xxx family of analog-digital converters (ADC). Including:
 *
 * <p>ADS1013, ADS1014, ADS1015 (12-bit)
 * <p>ADS1113, ADS1114, ADS1115 (16-bit)
 */
@SuppressWarnings("WeakerAccess")
public class Ads1xxx implements AutoCloseable {

    /**
     * Definition of specific ADC chip properties
     * that are not common across the entire device family.
     */
    public enum Configuration {
        ADS1013(12),
        ADS1014(12),
        ADS1015(12),
        ADS1113(16),
        ADS1114(16),
        ADS1115(16);

        final int resolution;
        Configuration(int resolution) {
            this.resolution = resolution;
        }

        /**
         * Return the voltage increment of the least significant bit (LSB)
         */
        private double getLsbSize(int range) {
            double countRange = Math.pow(2, resolution-1);
            switch (range) {
                case RANGE_6_144V:
                    return 6.144f / countRange;
                case RANGE_4_096V:
                    return 4.096f / countRange;
                case RANGE_2_048V:
                    return 2.048f / countRange;
                case RANGE_1_024V:
                    return 1.024f / countRange;
                case RANGE_0_512V:
                    return 0.512f / countRange;
                case RANGE_0_256V:
                    return 0.256f / countRange;
                default:
                    throw new IllegalArgumentException("Invalid input range value detected");
            }
        }
    }


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RANGE_0_256V, RANGE_0_512V, RANGE_1_024V, RANGE_2_048V, RANGE_4_096V, RANGE_6_144V})
    public @interface InputRange {}
    /**
     * Set the full scale range to 6.144V
     */
    public static final int RANGE_6_144V = 0x00;
    /**
     * Set the full scale range to 4.096V
     */
    public static final int RANGE_4_096V = 0x01;
    /**
     * Set the full scale range to 2.048V
     */
    public static final int RANGE_2_048V = 0x02;
    /**
     * Set the full scale range to 1.024V
     */
    public static final int RANGE_1_024V = 0x03;
    /**
     * Set the full scale range to 0.512V
     */
    public static final int RANGE_0_512V = 0x04;
    /**
     * Set the full scale range to 0.256V
     */
    public static final int RANGE_0_256V = 0x05;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({COMPARATOR_DISABLED, COMPARATOR_MODE_4, COMPARATOR_MODE_2, COMPARATOR_MODE_1})
    public @interface ComparatorMode {}
    /**
     * Trigger comparator output with a single sample outside the threshold.
     */
    public static final int COMPARATOR_MODE_1 = 0x0;
    /**
     * Trigger comparator output with two samples outside the threshold.
     */
    public static final int COMPARATOR_MODE_2 = 0x1;
    /**
     * Trigger comparator output with a four samples outside the threshold.
     */
    public static final int COMPARATOR_MODE_4 = 0x2;
    /**
     * Disable the comparator output.
     */
    public static final int COMPARATOR_DISABLED = 0x3;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INPUT_DIFF_0P_1N, INPUT_DIFF_0P_3N, INPUT_DIFF_1P_3N, INPUT_DIFF_2P_3N})
    public @interface DifferentialMode {}
    /**
     * Differential input between AIN0/AIN1.
     * AIN0 as IN+.
     */
    public static final int INPUT_DIFF_0P_1N = 0x0;
    /**
     * Differential input between AIN0/AIN3.
     * AIN0 as IN+.
     */
    public static final int INPUT_DIFF_0P_3N = 0x1;
    /**
     * Differential input between AIN1/AIN3.
     * AIN1 as IN+.
     */
    public static final int INPUT_DIFF_1P_3N = 0x2;
    /**
     * Differential input between AIN2/AIN3.
     * AIN2 as IN+.
     */
    public static final int INPUT_DIFF_2P_3N = 0x3;


    /**
     * Default I2C slave address
     */
    public static final int DEFAULT_ADDRESS = 0x48;


    /* ADS1xxx Register Map */
    private static final byte REG_CONVERSION = 0x00;
    private static final byte REG_CONFIG = 0x01;
    private static final byte REG_LO_THRESH = 0x02;
    private static final byte REG_HI_THRESH = 0x03;

    private static final int FLAG_SINGLE_ENDED = 0x4;
    private static final int FLAG_START_READ = 0x8000;
    private static final int RANGE_MASK = 0x0E00;
    private static final int INPUT_MASK = 0x7000;
    private static final int MODE_MASK = 0x0100;
    private static final int RATE_MASK = 0x7000;
    private static final int COMPARATOR_MASK = 0x3;


    private final Configuration mChipConfiguration;
    private I2cDevice mDevice;

    /**
     * Create a new Ads1xxx interface, with a slave address of {@link #DEFAULT_ADDRESS}.
     *
     * @param i2cName I2C port name where the device is attached. Cannot be null.
     * @param chip identifier for the connected device chip.
     * @throws IOException If the I2cDevice fails to open.
     */
    public Ads1xxx(String i2cName, Configuration chip) throws IOException {
        this(i2cName, DEFAULT_ADDRESS, chip);
    }

    /**
     * Create a new Ads1xxx interface.
     *
     * @param i2cName I2C port name where the device is attached. Cannot be null.
     * @param i2cAddress I2C slave address for the connected device.
     * @param chip identifier for the connected device.
     * @throws IOException If the I2cDevice fails to open.
     */
    public Ads1xxx(String i2cName, int i2cAddress, Configuration chip) throws IOException {
        PeripheralManager manager = PeripheralManager.getInstance();
        I2cDevice i2cDevice = manager.openI2cDevice(i2cName, i2cAddress);
        mChipConfiguration = chip;
        init(i2cDevice);
    }

    @VisibleForTesting
    /* package */ Ads1xxx(I2cDevice i2cDevice, Configuration chip) throws IOException {
        mChipConfiguration = chip;
        init(i2cDevice);
    }

    private void init(I2cDevice i2cDevice) throws IOException {
        if (i2cDevice == null) {
            throw new IllegalArgumentException("Must provide a valid I2C device");
        }

        mDevice = i2cDevice;

        // Ensure chip is in one-shot mode
        short config = getConfigRegister();
        config |= MODE_MASK;
        setConfigRegister(config);
    }

    /**
     * Set the programmable gain amplifier (PGA) to adjust the
     * full-scale input range of the device.
     *
     * <p>This configuration is only available on the ADS1x14 and ADS1x15 devices.
     * <p>Default is {@link #RANGE_2_048V}
     *
     * @param range one of {@link #RANGE_4_096V}, {@link #RANGE_2_048V}, {@link #RANGE_1_024V},
     *              {@link #RANGE_0_512V}, or {@link #RANGE_0_256V}
     */
    public void setInputRange(@InputRange int range) throws IOException {
        if (mChipConfiguration == Configuration.ADS1013
                || mChipConfiguration == Configuration.ADS1113) {
            throw new UnsupportedOperationException("ADS1x13 chips do not support input range");
        }

        short config = getConfigRegister();
        config &= ~RANGE_MASK;
        config |= (range << 9) & RANGE_MASK;
        setConfigRegister(config);
    }

    /**
     * Return the current full-scale input range of the device.
     *
     * @return one of {@link #RANGE_4_096V}, {@link #RANGE_2_048V}, {@link #RANGE_1_024V},
     *              {@link #RANGE_0_512V}, or {@link #RANGE_0_256V}
     * @see #setInputRange(int)
     */
    public @InputRange int getInputRange() throws IOException {
        if (mChipConfiguration == Configuration.ADS1013
                || mChipConfiguration == Configuration.ADS1113) {
            throw new UnsupportedOperationException("ADS1x13 chips do not support input range");
        }

        short config = getConfigRegister();
        return (config & RANGE_MASK) >>> 9;
    }

    /**
     * Configure the input threshold comparator on the device.
     * If enabled, the ALERT/RDY pin on the device will assert (active low)
     * when input samples fall outside the defined high/low thresholds.
     *
     * <p>This configuration is only available on the ADS1x14 and ADS1x15 devices.
     * <p>Default is {@link #COMPARATOR_DISABLED}
     *
     * @param mode one of {@link #COMPARATOR_MODE_1}, {@link #COMPARATOR_MODE_2},
     *             {@link #COMPARATOR_MODE_4}, or {@link #COMPARATOR_DISABLED}
     */
    public void setComparatorMode(@ComparatorMode int mode) throws IOException {
        if (mChipConfiguration == Configuration.ADS1013
                || mChipConfiguration == Configuration.ADS1113) {
            throw new UnsupportedOperationException("ADS1x13 chips do not support comparators");
        }

        short config = getConfigRegister();
        config &= ~COMPARATOR_MASK;
        config |= mode & COMPARATOR_MASK;
        setConfigRegister(config);
    }

    /**
     * Return the current threshold comparator mode set on the device.
     *
     * @return one of {@link #COMPARATOR_MODE_1}, {@link #COMPARATOR_MODE_2},
     *             {@link #COMPARATOR_MODE_4}, or {@link #COMPARATOR_DISABLED}
     * @see #setComparatorMode(int)
     */
    public @ComparatorMode int getComparatorMode() throws IOException {
        if (mChipConfiguration == Configuration.ADS1013
                || mChipConfiguration == Configuration.ADS1113) {
            throw new UnsupportedOperationException("ADS1x13 chips do not support comparators");
        }

        short config = getConfigRegister();
        return (config & COMPARATOR_MASK) >>> 0;
    }

    /**
     * Set the active input window when using comparator mode on the device.
     *
     * <p>This configuration is only available on the ADS1x14 and ADS1x15 devices.
     *
     * @param loThresh Low input threshold, expressed in raw counts.
     * @param hiThresh High input threshold, expressed in raw counts.
     */
    public void setComparatorThreshold(short loThresh, short hiThresh) throws IOException {
        if (mChipConfiguration == Configuration.ADS1013
                || mChipConfiguration == Configuration.ADS1113) {
            throw new UnsupportedOperationException("ADS1x13 chips do not support comparators");
        }
        mDevice.writeRegWord(REG_LO_THRESH, loThresh);
        mDevice.writeRegWord(REG_HI_THRESH, hiThresh);
    }

    /**
     * Perform a single-ended read on the requested input channel.
     * @param channel Input channel to read
     * @return ADC reading between +/- 2048 (12-bit) or +/- 32768 (16-bit)
     */
    public int readSingleEndedInput(int channel) throws IOException {
        if (mChipConfiguration != Configuration.ADS1015
                && mChipConfiguration != Configuration.ADS1115) {
            throw new UnsupportedOperationException("Single ended input only supported on ADS1x15 chips");
        }
        if (channel > 3) {
            throw new IllegalArgumentException("Channel cannot be greater than 3");
        }

        int mode = FLAG_SINGLE_ENDED | channel;

        short config = getConfigRegister();
        // set the input mode
        config &= ~INPUT_MASK;
        config |= (mode << 12) & INPUT_MASK;
        config |= FLAG_START_READ; // set the start read bit
        setConfigRegister(config);

        return readRawValue();
    }

    /**
     * Perform a single-ended read operation, and return the approximate voltage based on the
     * current full-scale input voltage range.
     *
     * @param channel Input channel to read
     * @return Scaled voltage value for the input
     * @see #setInputRange(int)
     * @see #readSingleEndedInput(int)
     */
    public double readSingleEndedVoltage(int channel) throws IOException {
        int range = getInputRange();
        double scaler = mChipConfiguration.getLsbSize(range);
        int rawValue = readSingleEndedInput(channel);

        return rawValue * scaler;
    }

    /**
     * Perform a differential read using the requested mode.
     * @param mode one of {@link #INPUT_DIFF_0P_1N}, {@link #INPUT_DIFF_0P_3N},
     *             {@link #INPUT_DIFF_1P_3N}, or {@link #INPUT_DIFF_2P_3N}.
     * @return ADC reading between +/- 2048 (12-bit) or +/- 32768 (16-bit)
     */
    public int readDifferentialInput(@DifferentialMode int mode) throws IOException {
        if ((mChipConfiguration == Configuration.ADS1013
                || mChipConfiguration == Configuration.ADS1113)
                && mode != INPUT_DIFF_0P_1N) {
            throw new UnsupportedOperationException("ADS1x13 chips only support INPUT_DIFF_0P_1N mode");
        }
        if ((mChipConfiguration == Configuration.ADS1014
                || mChipConfiguration == Configuration.ADS1114)
                && mode != INPUT_DIFF_0P_1N) {
            throw new UnsupportedOperationException("ADS1x14 chips only support INPUT_DIFF_0P_1N mode");
        }

        short config = getConfigRegister();
        // set the input mode
        config &= ~INPUT_MASK;
        config |= (mode << 12) & INPUT_MASK;
        config |= FLAG_START_READ; // set the start read bit
        setConfigRegister(config);

        return readRawValue();
    }

    /**
     * Perform a differential read operation, and return the approximate voltage based on the
     * current full-scale input voltage range.
     *
     * @param mode one of {@link #INPUT_DIFF_0P_1N}, {@link #INPUT_DIFF_0P_3N},
     *             {@link #INPUT_DIFF_1P_3N}, or {@link #INPUT_DIFF_2P_3N}.
     * @return Scaled voltage value for the input
     * @see #setInputRange(int)
     * @see #readDifferentialInput(int)
     */
    public double readDifferentialVoltage(@DifferentialMode int mode) throws IOException {
        int range = getInputRange();
        double scaler = mChipConfiguration.getLsbSize(range);
        int rawValue = readDifferentialInput(mode);

        return rawValue * scaler;
    }

    /**
     * Return the raw A/D conversion value
     */
    private int readRawValue() throws IOException {
        byte[] result = new byte[2];
        mDevice.readRegBuffer(REG_CONVERSION, result, 2);

        int rawValue = (result[0] << 8) | (result[1] & 0xFF);
        // Allow for sign extension, value is signed 2's complement
        if (mChipConfiguration.resolution == 12) {
            return (rawValue >> 4);
        } else {
            return rawValue;
        }
    }

    private short getConfigRegister() throws IOException {
        byte[] _configBytes = new byte[2];
        mDevice.readRegBuffer(REG_CONFIG, _configBytes, 2);

        return (short) (((_configBytes[0] & 0xFF) << 8) | (_configBytes[1] & 0xFF));
    }

    private void setConfigRegister(short configValue) throws IOException {
        byte[] _configBytes = new byte[]{
                (byte) ((configValue >>> 8) & 0xff),
                (byte) (configValue & 0xff)
        };

        mDevice.writeRegBuffer(REG_CONFIG, _configBytes, 2);
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
