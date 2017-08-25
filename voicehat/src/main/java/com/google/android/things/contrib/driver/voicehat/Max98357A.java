/*
 * Copyright 2017, The Android Open Source Project
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

package com.google.android.things.contrib.driver.voicehat;

import android.media.AudioFormat;
import android.support.annotation.VisibleForTesting;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2sDevice;
import com.google.android.things.pio.PeripheralManagerService;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Driver for audio chip Max98357A
 * <br>
 * <a href='https://datasheets.maximintegrated.com/en/ds/MAX98357A-MAX98357B.pdf'>Read the datasheet</a>
 */
public class Max98357A implements AutoCloseable {
    /**
     * Enables the audio chip.
     */
    public static final boolean SD_MODE_LEFT = true;
    /**
     * Puts the audio chip in shutdown mode.
     */
    public static final boolean SD_MODE_SHUTDOWN = false;
    /**
     * Provides a logic HIGH / Vdd signal to the gain slot pin.
     */
    public static final boolean GAIN_SLOT_ENABLE = true;
    /**
     * Provides a logic LOW / GND signal to the gain slot pin.
     */
    public static final boolean GAIN_SLOT_DISABLE = false;

    private final String mI2sBus;
    private final I2sDevice mDevice;
    private Gpio mNotSdModeGpio;
    private Gpio mGainSlotGpio;

    /**
     * Creates an interface to the chip. This constructor should only be used if this class is
     * being constructed without the {@link VoiceHat}. Otherwise, this class will be unable to access the
     * I2s bus since it will be locked by the VoiceHat class.
     *
     * @param i2sBus The name of the I2s bus to use
     * @param notSdMode The name for the NOT_SD_MODE pin
     * @param gainSlot The name for the GAIN_SLOT pin
     */
    public Max98357A(String i2sBus, String notSdMode, String gainSlot, AudioFormat audioFormat)
            throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        mI2sBus = i2sBus;
        mDevice = pioService.openI2sDevice(i2sBus, audioFormat);
        if (notSdMode != null) {
            mNotSdModeGpio = pioService.openGpio(notSdMode);
            mNotSdModeGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        }
        if (gainSlot != null) {
            mGainSlotGpio = pioService.openGpio(gainSlot);
            mGainSlotGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        }
    }

    /**
     * Creates an interface to the chip.
     *
     * @param device A pre-configured I2s bus
     * @param notSdMode The name for the NOT_SD_MODE pin
     * @param gainSlot The name for the GAIN_SLOT pin
     */
    public Max98357A(I2sDevice device, String notSdMode, String gainSlot) throws IOException {
        mI2sBus = null;
        mDevice = device;
        PeripheralManagerService pioService = new PeripheralManagerService();
        if (notSdMode != null) {
            mNotSdModeGpio = pioService.openGpio(notSdMode);
            mNotSdModeGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        }
        if (gainSlot != null) {
            mGainSlotGpio = pioService.openGpio(gainSlot);
            mGainSlotGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        }
    }

    @VisibleForTesting
    /* package */ I2sDevice getI2sDevice() {
        return mDevice;
    }

    @VisibleForTesting
    /* package */ Gpio getNotSdModePin() {
        return mNotSdModeGpio;
    }

    @VisibleForTesting
    /* package */ Gpio getGainSlotPin() {
        return mGainSlotGpio;
    }

    /**
     * Sets the mode of the chip.
     *
     * @param mode Either {@link #SD_MODE_LEFT} or {@link #SD_MODE_SHUTDOWN}.
     */
    public void setSdMode(boolean mode) throws IOException {
        if (mNotSdModeGpio == null) {
            throw new IOException("Pin not defined");
        }
        mNotSdModeGpio.setValue(mode);
    }

    /**
     * Sets the gain slot.

     * @param gainSlot Either {@link #GAIN_SLOT_ENABLE} or {@link #GAIN_SLOT_DISABLE}.
     */
    public void setGainSlot(boolean gainSlot) throws IOException {
        if (mGainSlotGpio == null) {
            throw new IOException("Pin not defined");
        }
        mGainSlotGpio.setValue(gainSlot);
    }

    /**
     * Sends passthrough I2s data to the device.
     *
     * @param data I2s data to be sent to chip.
     */
    public int writeI2sData(byte[] data) throws IOException {
        return mDevice.write(data, 0, data.length);
    }

    /**
     * Sends passthrough I2s data to the device.
     *
     * @param byteBuffer I2s data to be sent to chip.
     * @param i Offset
     */
    public int writeI2sData(ByteBuffer byteBuffer, int i) throws IOException {
        return mDevice.write(byteBuffer, i);
    }

    /**
     * Closes GPIO pins and the I2s bus.
     * @throws Exception
     */
    @Override
    public void close() throws IOException {
        if (mI2sBus != null && mDevice != null) {
            // This class owns the I2sDevice and is responsible for closing.
            mDevice.close();
        }
        mNotSdModeGpio.close();
        mGainSlotGpio.close();
    }
}
