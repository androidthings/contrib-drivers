/*
 * Copyright 2017 Google Inc.
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

package com.google.android.things.contrib.driver.vcnl4200;

import android.support.annotation.IntDef;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the VCNL4200, a module that serves as both a high-sensitivity
 * long distance proximity sensor and an ambient light sensor. The datasheet
 * is available at: https://www.vishay.com/docs/84430/vcnl4200.pdf.
 */
public class Vcnl4200 implements AutoCloseable {
    private static final String TAG = Vcnl4200.class.getSimpleName();

    /**
     * I2C Slave Address of the VCNL4200
     */
    private static final int I2C_ADDRESS = 0x51;

    /**
     * I2S Register Addresses
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REGISTER_ALS_CONF, REGISTER_ALS_HIGH_INT_THRESH, REGISTER_ALS_LOW_INT_THRESH,
            REGISTER_PS_CONF_1_2, REGISTER_PS_CONF_3_MS, REGISTER_PS_CANC_LEVEL,
            REGISTER_PROX_LOW_INT_THRESH, REGISTER_PROX_HIGH_INT_THRESH, REGISTER_PROX_DATA,
            REGISTER_ALS_DATA, REGISTER_WHITE_DATA, REGISTER_INTERRUPT_FLAGS, REGISTER_DEVICE_ID})
    /*package*/ @interface Register {}

    static final int REGISTER_ALS_CONF               = 0x00;
    static final int REGISTER_ALS_HIGH_INT_THRESH    = 0x01;
    static final int REGISTER_ALS_LOW_INT_THRESH     = 0x02;
    static final int REGISTER_PS_CONF_1_2            = 0x03;
    static final int REGISTER_PS_CONF_3_MS           = 0x04;
    static final int REGISTER_PS_CANC_LEVEL          = 0x05;
    static final int REGISTER_PROX_LOW_INT_THRESH    = 0x06;
    static final int REGISTER_PROX_HIGH_INT_THRESH   = 0x07;
    static final int REGISTER_PROX_DATA              = 0x08;
    static final int REGISTER_ALS_DATA               = 0x09;
    static final int REGISTER_WHITE_DATA             = 0x0A;
    static final int REGISTER_INTERRUPT_FLAGS        = 0x0D;
    static final int REGISTER_DEVICE_ID              = 0x0E;

    /**
     * ALS Integration Time Values
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_IT_TIME_50MS, ALS_IT_TIME_100MS, ALS_IT_TIME_200MS, ALS_IT_TIME_400MS})
    public @interface AlsIntegrationTime {}

    public static final int ALS_IT_TIME_50MS    = 0;
    public static final int ALS_IT_TIME_100MS   = 0b01 << 6;
    public static final int ALS_IT_TIME_200MS   = 0b10 << 6;
    public static final int ALS_IT_TIME_400MS   = 0b11 << 6;

    private static final int ALS_IT_TIME_MASK   = 0b11 << 6;

    /**
     * ALS Interrupt Switch Values
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_INT_SWITCH_ALS_CHANNEL, ALS_INT_SWITCH_WHITE_CHANNEL})
    public @interface AlsInterruptSwitch {}

    public static final int ALS_INT_SWITCH_ALS_CHANNEL    = 0;
    public static final int ALS_INT_SWITCH_WHITE_CHANNEL  = 0b1 << 5;

    private static final int ALS_INT_SWITCH_MASK          = 0b1 << 5;

    /**
     * ALS Interrupt Persistence Setting Values
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_INT_PERSISTENCE_1, ALS_INT_PERSISTENCE_2, ALS_INT_PERSISTENCE_4,
            ALS_INT_PERSISTENCE_8})
    public @interface AlsInterruptPersistence {}

    public static final int ALS_INT_PERSISTENCE_1 = 0;
    public static final int ALS_INT_PERSISTENCE_2 = 0b01 << 2;
    public static final int ALS_INT_PERSISTENCE_4 = 0b10 << 2;
    public static final int ALS_INT_PERSISTENCE_8 = 0b11 << 2;

    private static final int ALS_INT_PERSISTENCE_MASK = 0b11 << 2;

    /**
     * ALS Interrupt Setting
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_INT_DISABLE, ALS_INT_ENABLE})
    public @interface AlsInterrupt {}

    public static final int ALS_INT_DISABLE = 0;
    public static final int ALS_INT_ENABLE  = 0b1 << 1;

    private static final int ALS_INT_MASK   = 0b1 << 1;

    /**
     * ALS Power Setting
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_POWER_ON, ALS_POWER_OFF})
    public @interface AlsPower {}

    public static final int ALS_POWER_ON  = 0;
    public static final int ALS_POWER_OFF = 1;

    private static final int ALS_POWER_MASK = 1;

    /**
     * PS Duty Cycles
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_IRED_DUTY_CYCLE_1_160, PS_IRED_DUTY_CYCLE_1_320, PS_IRED_DUTY_CYCLE_1_640,
            PS_IRED_DUTY_CYCLE_1_1280})
    public @interface PsIredDutyCycle {}

    public static final int PS_IRED_DUTY_CYCLE_1_160    = 0;
    public static final int PS_IRED_DUTY_CYCLE_1_320    = 0b01 << 6;
    public static final int PS_IRED_DUTY_CYCLE_1_640    = 0b10 << 6;
    public static final int PS_IRED_DUTY_CYCLE_1_1280   = 0b11 << 6;

    private static final int PS_IRED_DUTY_CYCLE_MASK    = 0b11 << 6;

    /**
     * PS Interrupt Persistence Setting
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_INT_PERSISTENCE_1, PS_INT_PERSISTENCE_2, PS_INT_PERSISTENCE_3,
            PS_INT_PERSISTENCE_4})
    public @interface PsInterruptPersistence {}

    public static final int PS_INT_PERSISTENCE_1    = 0;
    public static final int PS_INT_PERSISTENCE_2    = 0b01 << 4;
    public static final int PS_INT_PERSISTENCE_3    = 0b10 << 4;
    public static final int PS_INT_PERSISTENCE_4    = 0b11 << 4;

    private static final int PS_INT_PERSISTENCE_MASK = 0b11 << 4;

    /**
     * PS Interrupt Persistence Setting
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_IT_1, PS_IT_1_5, PS_IT_2, PS_IT_4, PS_IT_8, PS_IT_9})
    public @interface PsIntegrationTime {}

    public static final int PS_IT_1     = 0;
    public static final int PS_IT_1_5   = 0b001 << 1;
    public static final int PS_IT_2     = 0b010 << 1;
    public static final int PS_IT_4     = 0b011 << 1;
    public static final int PS_IT_8     = 0b100 << 1;
    public static final int PS_IT_9     = 0b101 << 1;

    private static final int PS_IT_MASK = 0b111 << 1;

    /**
     * PS Power Setting
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_POWER_ON, PS_POWER_OFF})
    public @interface PsPower {}

    public static final int PS_POWER_ON  = 0;
    public static final int PS_POWER_OFF = 1;

    private static final int PS_POWER_MASK = 1;

    /**
     * PS Output Resolution
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_OUT_RES_12_BITS, PS_OUT_RES_16_BITS})
    public @interface PsOutputResolution {}

    public static final int PS_OUT_RES_12_BITS  = 0;
    public static final int PS_OUT_RES_16_BITS  = 1 << 11;

    private static final int PS_OUT_RES_MASK    = 1 << 11;

    /**
     * PS Output Resolution
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_INT_CONFIG_DISABLE, PS_INT_CONFIG_TRIGGER_BY_AWAY,
            PS_INT_CONFIG_TRIGGER_BY_CLOSING, PS_INT_CONFIG_TRIGGER_BY_CLOSING_AND_AWAY})
    public @interface PsInterruptConfiguration {}

    public static final int PS_INT_CONFIG_DISABLE                     = 0b00;
    public static final int PS_INT_CONFIG_TRIGGER_BY_CLOSING          = 0b01 << 8;
    public static final int PS_INT_CONFIG_TRIGGER_BY_AWAY             = 0b10 << 8;
    public static final int PS_INT_CONFIG_TRIGGER_BY_CLOSING_AND_AWAY = 0b11 << 8;

    private static final int PS_INT_CONFIG_MASK                       = 0b11 << 8;

    /**
     * PS Multi-Pulse Numbers
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_MULTI_PULSE_1, PS_MULTI_PULSE_2, PS_MULTI_PULSE_4, PS_MULTI_PULSE_8})
    public @interface PsMultiPulseNumbers {}

    public static final int PS_MULTI_PULSE_1 = 0b00;
    public static final int PS_MULTI_PULSE_2 = 0b01;
    public static final int PS_MULTI_PULSE_4 = 0b10;
    public static final int PS_MULTI_PULSE_8 = 0b11;

    private static final int PS_MULTI_PULSE_MASK = 0b11;

    /**
     * PS Smart Persistence
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_SMART_PERSISTENCE_DISABLE, PS_SMART_PERSISTENCE_ENABLE})
    public @interface PsSmartPersistence {}

    public static final int PS_SMART_PERSISTENCE_DISABLE = 0;
    public static final int PS_SMART_PERSISTENCE_ENABLE  = 1 << 4;

    private static final int PS_SMART_PERSISTENCE_MASK   = 1 << 4;

    /**
     * PS Active Force Mode
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_ACTIVE_FORCE_MODE_DISABLE, PS_ACTIVE_FORCE_MODE_ENABLE})
    public @interface PsActiveForceMode {}

    public static final int PS_ACTIVE_FORCE_MODE_DISABLE = 0;
    public static final int PS_ACTIVE_FORCE_MODE_ENABLE  = 1 << 3;

    private static final int PS_ACTIVE_FORCE_MODE_MASK   = 1 << 3;

    /**
     * PS Trigger Mode
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_TRIGGER_NO_PS_ACTIVE_FORCE_MODE, PS_TRIGGER_ONE_TIME_CYCLE})
    public @interface PsTriggerMode {}

    public static final int PS_TRIGGER_NO_PS_ACTIVE_FORCE_MODE = 0;
    public static final int PS_TRIGGER_ONE_TIME_CYCLE          = 1 << 2;

    private static final int PS_TRIGGER_MASK                   = 1 << 2;

    /**
     * PS Sunlight Immunity
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_SUNLIGHT_IMMUNITY_TYPICAL, PS_SUNLIGHT_IMMUNITY_2X})
    public @interface PsSunlightImmunity {}

    public static final int PS_SUNLIGHT_IMMUNITY_TYPICAL = 0;
    public static final int PS_SUNLIGHT_IMMUNITY_2X      = 1 << 1;

    private static final int PS_SUNLIGHT_IMMUNITY_MASK   = 1 << 1;

    /**
     * PS Sunlight Cancellation Function
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_SUNLIGHT_CANC_DISABLE, PS_SUNLIGHT_CANC_ENABLE})
    public @interface PsSunlightCancellation {}

    public static final int PS_SUNLIGHT_CANC_DISABLE = 0;
    public static final int PS_SUNLIGHT_CANC_ENABLE  = 1;

    private static final int PS_SUNLIGHT_CANC_MASK   = 1;

    /**
     * PS Operation Mode
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_OP_NORMAL_WITH_INT, PS_OP_DETECT_LOGIC_OUTPUT})
    public @interface PsOperationMode {}

    public static final int PS_OP_NORMAL_WITH_INT     = 0;
    public static final int PS_OP_DETECT_LOGIC_OUTPUT = 1 << 13;

    private static final int PS_OP_MASK               = 1 << 13;

    /**
     * PS Sunlight Capability
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_SUNLIGHT_CAP_TYPICAL, PS_SUNLIGHT_CAP_1_5})
    public @interface PsSunlightCapability {}

    public static final int PS_SUNLIGHT_CAP_TYPICAL = 0;
    public static final int PS_SUNLIGHT_CAP_1_5     = 1 << 12;

    private static final int PS_SUNLIGHT_CAP_MASK   = 1 << 12;

    /**
     * PS Sunlight Protect Mode
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_SUNLIGHT_PROTECT_MODE_00, PS_SUNLIGHT_PROTECT_MODE_FF})
    public @interface PsSunlightProtectMode {}

    public static final int PS_SUNLIGHT_PROTECT_MODE_00 = 0;
    public static final int PS_SUNLIGHT_PROTECT_MODE_FF = 1 << 11;

    private static final int PS_SUNLIGHT_PROTECT_MASK   = 1 << 11;

    /**
     * PS LED Current
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PS_LED_CURRENT_50MA, PS_LED_CURRENT_75MA, PS_LED_CURRENT_100MA, PS_LED_CURRENT_120MA,
            PS_LED_CURRENT_140MA, PS_LED_CURRENT_160MA, PS_LED_CURRENT_180MA, PS_LED_CURRENT_200MA})
    public @interface PsLedCurrent {}

    public static final int PS_LED_CURRENT_50MA      = 0;
    public static final int PS_LED_CURRENT_75MA      = 0b001 << 8;
    public static final int PS_LED_CURRENT_100MA     = 0b010 << 8;
    public static final int PS_LED_CURRENT_120MA     = 0b011 << 8;
    public static final int PS_LED_CURRENT_140MA     = 0b100 << 8;
    public static final int PS_LED_CURRENT_160MA     = 0b101 << 8;
    public static final int PS_LED_CURRENT_180MA     = 0b110 << 8;
    public static final int PS_LED_CURRENT_200MA     = 0b111 << 8;

    private static final int PS_LED_CURRENT_MASK     = 0b111 << 8;

    /**
     * Interrupt Status Register
     */
    public static class InterruptStatus {
        public final boolean FLAG_PS_UPFLAG;
        public final boolean FLAG_PS_SPFLAG;
        public final boolean FLAG_ALS_IF_L;
        public final boolean FLAG_ALS_IF_H;
        public final boolean FLAG_PS_IF_CLOSE;
        public final boolean FLAG_PS_IF_AWAY;

        private InterruptStatus(short status) {
            status = (short)((status >> 8) & 0xFF);
            FLAG_PS_IF_AWAY = isBitSet(status, 0);
            FLAG_PS_IF_CLOSE = isBitSet(status, 1);
            FLAG_ALS_IF_H = isBitSet(status, 4);
            FLAG_ALS_IF_L = isBitSet(status, 5);
            FLAG_PS_SPFLAG = isBitSet(status, 6);
            FLAG_PS_UPFLAG = isBitSet(status, 7);
        }

        private boolean isBitSet(short word, int bitIndex) {
            return ((word >> bitIndex) & 1) != 0;
        }

        static InterruptStatus fromStatus(short status) {
            return new InterruptStatus(status);
        }
    }

    public static final int PS_MAX_DATA_VALUE = 0xFFFF;
    // Ambient light resolution is affected by the integration time.
    public static final float[] ALS_IT_50MS_SENSITIVITY_RANGE  = { 0.024f, 1573 };
    public static final float[] ALS_IT_100MS_SENSITIVITY_RANGE = { 0.012f,  786 };
    public static final float[] ALS_IT_200MS_SENSITIVITY_RANGE = { 0.006f,  393 };
    public static final float[] ALS_IT_400MS_SENSITIVITY_RANGE = { 0.003f,  197 };

    private I2cDevice mDevice;
    private float mAlsResolution;
    private float mAlsMaxRange;

    /**
     * Create a new VCNL4200 sensor driver connected to the given I2C bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Vcnl4200(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
        try {
            connect(device);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new VCNL4200 sensor driver connected to the given I2C device.
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    /*package*/ Vcnl4200(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        if (mDevice != null) {
            throw new IllegalStateException("device already connected");
        }
        mDevice = device;
        resetAlsConfiguration();
        resetPsConfiguration();
        updateLocalAlsConfiguration();
    }

    /**
     * Zeros out configuration registers for the ambient light sensor. The following settings will
     * be applied as a consequence: ALSIntegrationTime (50ms), AlsInterruptSwitch(ALS),
     * AlsInterruptPersistence(1), AlsInterruptEnable(false), AlsPower(true).
     * @throws IOException
     * @throws IllegalStateException
     */
    public void resetAlsConfiguration() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        mDevice.writeRegWord(REGISTER_ALS_CONF, (short)0x00);
        updateLocalAlsConfiguration();
    }

    /**
     * Zeros out the configuration registers for the proximity sensor. The following settings will
     * be applied as a consequence: PsIredDutyCycle(1/160), PsInterruptPersistence(1),
     * PsIntegrationTime(1T), PsPower(true), PsOutputResolution(12),
     * PsInterruptConfiguration(disabled), PsMultiPulseNumbers(1), PsSmartPersistence(false),
     * PsActiveForceMode(false), PsTriggerMode(no-active-force), PsSunlightImmunity(typical),
     * PsSunlightCancellation(false), PsOperationMode(normal), PsSunlightCapability(typical),
     * PsSunlightProtected(0x00), PsLedCurrent(50mA).
     * @throws IOException
     * @throws IllegalStateException
     */
    public void resetPsConfiguration() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        mDevice.writeRegWord(REGISTER_PS_CONF_1_2, (short)0x00);
        mDevice.writeRegWord(REGISTER_PS_CONF_3_MS, (short)0x00);
    }

    /**
     * Returns Device ID - for MP version sample, the LSB is 0x58.
     * @return ID reported by the sensor.
     * @throws IOException
     * @throws IllegalStateException
     */
    public int getDeviceId() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        return mDevice.readRegWord(REGISTER_DEVICE_ID) & 0xFFFF;
    }

    /**
     * Returns ambient light sensor value. Larger counts suggest more illuminated environments.
     * @return Lux reported by sensor.
     * @throws IOException
     * @throws IllegalStateException
     */
    public float getAlsData() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        return (mDevice.readRegWord(REGISTER_ALS_DATA) & 0xFFFF) * mAlsResolution;
    }

    /**
     * The resolution of the ambient light sensor changes based on the current integration setting.
     * @return The resolution based on the last time the ALS configuration was set.
     */
    public float getCurrentAlsResolution() throws IOException, IllegalStateException {
        return mAlsResolution;
    }

    /**
     * The max range of the ambient light sensor changes based on the current integration setting.
     * @return The max range based on the last time the ALS configuration was set.
     */
    public float getCurrentAlsMaxRange() throws IOException, IllegalStateException {
        return mAlsMaxRange;
    }

    private void updateLocalAlsConfiguration() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        short alsConfig = mDevice.readRegWord(REGISTER_ALS_CONF);
        switch (alsConfig & ALS_IT_TIME_MASK) {
            case ALS_IT_TIME_50MS:
                mAlsResolution = ALS_IT_50MS_SENSITIVITY_RANGE[0];
                mAlsMaxRange = ALS_IT_50MS_SENSITIVITY_RANGE[1];
                break;
            case ALS_IT_TIME_100MS:
                mAlsResolution = ALS_IT_100MS_SENSITIVITY_RANGE[0];
                mAlsMaxRange = ALS_IT_100MS_SENSITIVITY_RANGE[1];
                break;
            case ALS_IT_TIME_200MS:
                mAlsResolution = ALS_IT_200MS_SENSITIVITY_RANGE[0];
                mAlsMaxRange = ALS_IT_200MS_SENSITIVITY_RANGE[1];
                break;
            case ALS_IT_TIME_400MS:
                mAlsResolution = ALS_IT_400MS_SENSITIVITY_RANGE[0];
                mAlsMaxRange = ALS_IT_400MS_SENSITIVITY_RANGE[1];
                break;
            default:
                throw new IllegalStateException("unexpected als integration time");
        }
    }

    /**
     * Returns proximity sensor value. Larger counts suggest closer proximity to the sensor.
     * @return Counts reported by sensor.
     * @throws IOException
     * @throws IllegalStateException
     */
    public int getPsData() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        return mDevice.readRegWord(REGISTER_PROX_DATA);
    }

    /**
     * Returns white data.
     * @return White channel data, reported in counts.
     * @throws IOException
     * @throws IllegalStateException
     */
    public int getWhiteData() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        return (mDevice.readRegWord(REGISTER_WHITE_DATA) & 0xFFFF);
    }

    /**
     * Returns interrupt register. Calling this function resets the interrupt pad.
     * @return An InterruptStatus class that holds the interrupt flags.
     * @throws IOException
     * @throws IllegalStateException
     */
    public InterruptStatus getInterruptStatus() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        return InterruptStatus.fromStatus(mDevice.readRegWord(REGISTER_INTERRUPT_FLAGS));
    }

    private void setAlsConfiguration(int configMask, int config)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        short alsConfig = (short)((mDevice.readRegWord(REGISTER_ALS_CONF) & ~configMask) | config);
        mDevice.writeRegWord(REGISTER_ALS_CONF, alsConfig);
    }

    /**
     * Sets the integration time for the ambient light sensor.
     * @param integrationTime Length of the sensor pulsing. Lower integration times will increase
     * the lower the resolution but increase the maximum detection range.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setAlsIntegrationTime(@AlsIntegrationTime int integrationTime)
            throws IOException, IllegalStateException {
        setAlsConfiguration(ALS_IT_TIME_MASK, integrationTime);
        updateLocalAlsConfiguration();
    }

    /**
     * Sets the interrupt switch for the ambient light sensor.
     * @param interruptSwitch Interrupt on ALS-channel or white-channel.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setAlsInterruptSwitch(@AlsInterruptSwitch int interruptSwitch)
            throws IOException, IllegalStateException {
        setAlsConfiguration(ALS_INT_SWITCH_MASK, interruptSwitch);
    }

    /**
     * Sets the interrupt persistence value for the ambient light sensor.
     * @param interruptPersistence Number of counts that must be exceeded before interrupt is set.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setAlsInterruptPersistence(@AlsInterruptPersistence int interruptPersistence)
            throws IOException, IllegalStateException {
        setAlsConfiguration(ALS_INT_PERSISTENCE_MASK, interruptPersistence);
    }

    /**
     * Enables/disables whether the ambient light sensor should send interrupts.
     * @param enable
     * @throws IOException
     * @throws IllegalStateException
     */
    public void enableAlsInterrupt(boolean enable)
            throws IOException, IllegalStateException {
        setAlsConfiguration(ALS_INT_MASK, (enable) ? ALS_INT_ENABLE : ALS_INT_DISABLE);
    }

    /**
     * Powers on/off the ambient light sensor.
     * @param enable
     * @throws IOException
     * @throws IllegalStateException
     */
    public void enableAlsPower(boolean enable)
            throws IOException, IllegalStateException {
        setAlsConfiguration(ALS_POWER_MASK, (enable) ? ALS_POWER_ON : ALS_POWER_OFF);
    }

    /**
     * Sets the low and high thresholds at which the interrupt is fired for ALS.
     * @param lowThreshold Lower boundary at which interrupt will fire, value in lux.
     * @param highThreshold Upper boundary at which interrupt will fire, value in lux.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setAlsInterruptThresholds(float lowThreshold, float highThreshold)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        } else if (lowThreshold > highThreshold) {
            throw new IllegalArgumentException("low threshold is greater than high threshold");
        } else if (lowThreshold < 0) {
            throw new IllegalArgumentException("low threshold must be positive");
        } else if (highThreshold > mAlsMaxRange) {
            throw new IllegalArgumentException("high threshold is set greater than the max range");
        }
        mDevice.writeRegWord(REGISTER_ALS_LOW_INT_THRESH,
                (short)(lowThreshold / mAlsResolution));
        mDevice.writeRegWord(REGISTER_ALS_HIGH_INT_THRESH,
                (short)(highThreshold / mAlsResolution));
    }

    private void setPsConf12(int configMask, int config)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        short psConfig12 =
                (short)((mDevice.readRegWord(REGISTER_PS_CONF_1_2) & ~configMask) | config);
        mDevice.writeRegWord(REGISTER_PS_CONF_1_2, psConfig12);
    }

    private void setPsConf3Ms(int configMask, int config)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        short psConfig3Ms =
                (short)((mDevice.readRegWord(REGISTER_PS_CONF_3_MS) & ~configMask) | config);
        mDevice.writeRegWord(REGISTER_PS_CONF_3_MS, psConfig3Ms);
    }

    /**
     * Sets proximity sensor IRED duty cycle.
     * @param dutyCycle IRED on/off duty ratio, lower ratios mean faster response but higher current
     * consumption, and vice-versa.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsIredDutyCycle(@PsIredDutyCycle int dutyCycle)
            throws IOException, IllegalStateException {
        setPsConf12(PS_IRED_DUTY_CYCLE_MASK, dutyCycle);
    }

    /**
     * Sets the interrupt persistence of the proximity sensor.
     * @param interruptPersistence Number of counts that must be exceeded before interrupt is set.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsInterruptPersistence(@PsInterruptPersistence int interruptPersistence)
            throws IOException, IllegalStateException {
        setPsConf12(PS_INT_PERSISTENCE_MASK, interruptPersistence);
    }

    /**
     * Sets the integration time of the proximity sensor.
     * @param integrationTime Length of the sensor pulsing.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsIntegrationTime(@PsIntegrationTime int integrationTime)
            throws IOException, IllegalStateException {
        setPsConf12(PS_IT_MASK, integrationTime);
    }

    /**
     * Enables/disables the proximity sensor.
     * @param enable
     * @throws IOException
     * @throws IllegalStateException
     */
    public void enablePsPower(boolean enable)
            throws IOException, IllegalStateException {
        setPsConf12(PS_POWER_MASK, (enable) ? PS_POWER_ON : PS_POWER_OFF);
    }

    /**
     * Sets the output resolution of the proximity sensor.
     * @param outputResolution Set to report either 12-bits or 16-bits of data.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsOutputResolution(@PsOutputResolution int outputResolution)
            throws IOException, IllegalStateException {
        setPsConf12(PS_OUT_RES_MASK, outputResolution);
    }

    /**
     * Sets the interrupt configuration of the proximity sensor.
     * @param interruptConfiguration Configure which thresholds the interrupt fires on.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsInterruptConfiguration(@PsInterruptConfiguration int interruptConfiguration)
            throws IOException, IllegalStateException {
        setPsConf12(PS_INT_CONFIG_MASK, interruptConfiguration);
    }

    /**
     * Sets number of pulses to fire per every defined time frame for the proximity sensor.
     * @param multiPulseNumbers Count of pulses. More pulses will lead to longer IRED on time, but
     * will increase the detection range.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsMultiPulseNumbers(@PsMultiPulseNumbers int multiPulseNumbers)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_MULTI_PULSE_MASK, multiPulseNumbers);
    }

    /**
     * Enable/disable the smart-persistence setting of the proximity sensor.
     * @param enable
     * @throws IOException
     * @throws IllegalStateException
     */
    public void enablePsSmartPersistence(boolean enable)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_SMART_PERSISTENCE_MASK,
                (enable) ? PS_SMART_PERSISTENCE_ENABLE : PS_SMART_PERSISTENCE_DISABLE);
    }

    /**
     * Enable/disable the active-force trigger (on-demand reading) for proximity sensor.
     * @param enable
     * @throws IOException
     * @throws IllegalStateException
     */
    public void enablePsActiveForceMode(boolean enable)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_ACTIVE_FORCE_MODE_MASK,
                (enable) ? PS_ACTIVE_FORCE_MODE_ENABLE : PS_ACTIVE_FORCE_MODE_DISABLE);
    }

    /**
     * When active-force mode is enabled, trigger a one-time cycle and read the data out.
     * @throws IOException
     * @throws IllegalStateException
     */
    public int getOnDemandPsData()
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_TRIGGER_MASK, PS_TRIGGER_ONE_TIME_CYCLE);
        return getPsData();
    }

    /**
     * Set sunlight immunity level.
     * @param sunlightImmunity Switch between normal and 2x sunlight immunity.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsSunlightImmunity(@PsSunlightImmunity int sunlightImmunity)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_SUNLIGHT_IMMUNITY_MASK, sunlightImmunity);
    }

    /**
     * Enable/disable whether proximity sensor actively cancels sunlight.
     * @param enable
     * @throws IOException
     * @throws IllegalStateException
     */
    public void enablePsCancellationFunction(boolean enable)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_SUNLIGHT_CANC_MASK,
                (enable) ? PS_SUNLIGHT_CANC_ENABLE : PS_SUNLIGHT_CANC_DISABLE);
    }

    /**
     * Sets whether interrupt pin is used as a proximity detection logic pin.
     * @param operationMode Switch between normal operation and detection logic.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsOperationMode(@PsOperationMode int operationMode)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_OP_MASK, operationMode);
    }

    /**
     * Sets sunlight capability of proximity sensor.
     * @param sunlightCapability Switch between typical capability and 1.5x capability.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsSunlightCapability(@PsSunlightCapability int sunlightCapability)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_SUNLIGHT_CAP_MASK, sunlightCapability);
    }

    /**
     * Sets sunlight protect mode configuration.
     * @param sunlightProtectMode Switches between reporting 0x00 and 0xFF in protect mode.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsSunlightProtectMode(@PsSunlightProtectMode int sunlightProtectMode)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_SUNLIGHT_PROTECT_MASK, sunlightProtectMode);
    }

    /**
     * Sets the LED current of the proximity sensor.
     * @param ledCurrent Sets the amount of current allowed to be used by the LED.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsLedCurrent(@PsLedCurrent int ledCurrent)
            throws IOException, IllegalStateException {
        setPsConf3Ms(PS_LED_CURRENT_MASK, ledCurrent);
    }

    /**
     * Set the cancellation level for the proximity sensor.
     * @param cancellationLevel Level to cancel out background light issues.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsCancellationLevel(short cancellationLevel)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        }
        mDevice.writeRegWord(REGISTER_PS_CANC_LEVEL, cancellationLevel);
    }

    /**
     * Sets the low and high thresholds at which the interrupt is fired for PS.
     * @param lowThreshold Lower boundary at which interrupt will fire, value in counts.
     * @param highThreshold Upper boundary at which interrupt will fire, value in counts.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setPsInterruptThresholds(int lowThreshold, int highThreshold)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device is not connected");
        } else if (lowThreshold > highThreshold) {
            throw new IllegalArgumentException("low threshold is greater than high threshold");
        } else if (lowThreshold < 0) {
            throw new IllegalArgumentException("low threshold must be positive");
        } else if (highThreshold > PS_MAX_DATA_VALUE) {
            throw new IllegalArgumentException("high threshold is greater than the max range");
        }
        mDevice.writeRegWord(REGISTER_PROX_LOW_INT_THRESH, (short)(lowThreshold & 0xFFFF));
        mDevice.writeRegWord(REGISTER_PROX_HIGH_INT_THRESH, (short)(highThreshold & 0xFFFF));
    }

    /**
     * Shuts down the underlying I2C device.
     * @throws IOException
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
