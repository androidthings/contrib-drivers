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

import android.support.annotation.VisibleForTesting;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import java.io.IOException;

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

    private Gpio mNotSdModeGpio;
    private Gpio mGainSlotGpio;

    /**
     * Creates an interface to the chip.
     *
     * @param notSdMode The name for the NOT_SD_MODE pin
     * @param gainSlot The name for the GAIN_SLOT pin
     */
    public Max98357A(String notSdMode, String gainSlot) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
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
     * Closes GPIO pins and the I2s bus.
     */
    @Override
    public void close() throws IOException {
        mNotSdModeGpio.close();
        mGainSlotGpio.close();
    }
}
