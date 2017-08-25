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

import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.Button.LogicState;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2sDevice;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.userdriver.AudioInputDriver;
import com.google.android.things.userdriver.AudioOutputDriver;
import com.google.android.things.userdriver.UserDriverManager;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class provides a driver to use the VoiceHat, which enables connection to a microphone and
 * speaker for audio input and output. In order to take full advantage of this feature, multiple
 * permissions should be added to the application manifest:
 *
 * <pre>
 * &lt;uses-permission android:name=&quot;android.permission.RECORD_AUDIO&quot; /&gt;
 * &lt;uses-permission android:name=&quot;android.permission.MODIFY_AUDIO_SETTINGS&quot; /&gt;
 * &lt;uses-permission android:name=&quot;com.google.android.things.permission.MANAGE_AUDIO_DRIVERS&quot; /&gt;
 * </pre>
 *
 * In order to use the pushbutton as an input driver, an additional permission must be added:
 *
 * <pre>
 * &lt;uses-permission android:name=&quot;com.google.android.things.permission.MANAGE_INPUT_DRIVERS&quot; /&gt;
 * </pre>
 */
public class VoiceHat implements AutoCloseable {
    private static final String TAG = VoiceHat.class.getSimpleName();

    // buffer of 0.05 sec of sample data at 48khz / 16bit.
    private static final int BUFFER_SIZE = 96000 / 20;
    // buffer of 0.5 sec of sample data at 48khz / 16bit.
    private static final int FLUSH_SIZE = 48000;
    private static final int BUTTON_DEBOUNCE_DELAY_MS = 20;

    private static final String RPI_I2S_BUS = "I2S1";
    private static final String RPI_TRIGGER_GPIO = "BCM16";
    private static final String RPI_BUTTON_GPIO = "BCM23";
    private static final String RPI_LED_GPIO = "BCM25";

    private I2sDevice mDevice;
    private Max98357A mDac;

    private AudioFormat mAudioFormat;
    private AudioInputUserDriver mAudioInputDriver;
    private AudioOutputUserDriver mAudioOutputDriver;

    /* package */ static void assertRaspberryPi3() {
        if (!Build.DEVICE.equals("rpi3")) {
            throw new RuntimeException("This device is not a Raspberry Pi 3, so it cannot use the d"
                + "efault pin names");
        }
    }

    /**
     * Opens the button using the default pin on the Raspberry Pi.
     *
     * @return Pushbutton on the VoiceHat
     */
    public static Button openButton() throws IOException {
        assertRaspberryPi3();
        return openButton(RPI_BUTTON_GPIO);
    }

    /**
     * Opens the button using a specified pin.
     *
     * @param pinName The pin attached to the button.
     * @return Pushbutton on the VoiceHat
     */
    public static Button openButton(String pinName) throws IOException {
        Button pushButton = new Button(pinName, LogicState.PRESSED_WHEN_LOW);
        pushButton.setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS);
        return pushButton;
    }

    /**
     * Opens the LED using the default pin on the Raspberry Pi.
     *
     * @return LED on the VoiceHat
     */
    public static Gpio openLed() throws IOException {
        assertRaspberryPi3();
        return openLed(RPI_LED_GPIO);
    }

    /**
     * Opens the LED using a specified pin.
     *
     * @return LED on the VoiceHat
     */
    public static Gpio openLed(String ledGpioPin) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Gpio led = pioService.openGpio(ledGpioPin);
        led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        return led;
    }

    /**
     * Opens an InputDriver using the default pin on the Raspberry Pi.
     *
     * @return InputDriver for the pushbutton on the VoiceHat
     */
    public static ButtonInputDriver createButtonInputDriver(int keyCode) throws IOException {
        assertRaspberryPi3();
        return createButtonInputDriver(RPI_BUTTON_GPIO, keyCode);
    }

    /**
     * Opens an InputDriver using a specified.
     *
     * @return InputDriver for the pushbutton on the VoiceHat
     */
    public static ButtonInputDriver createButtonInputDriver(String buttonGpioPin, int keyCode)
        throws IOException {
        return new ButtonInputDriver(buttonGpioPin, LogicState.PRESSED_WHEN_LOW, keyCode);
    }

    public VoiceHat(AudioFormat audioFormat)
            throws IOException {
        // Audio-only constructor
        this(RPI_I2S_BUS, RPI_TRIGGER_GPIO, audioFormat);
        assertRaspberryPi3();
    }

    public VoiceHat(String i2sBus, String ampEnablePin, AudioFormat audioFormat)
        throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mDevice = pioService.openI2sDevice(i2sBus, audioFormat);
            mAudioFormat = audioFormat;
            // On the VoiceHat, the GAIN_SLOT pin on the DAC is not connected.
            mDac = new Max98357A(mDevice, ampEnablePin, null);
        } catch (IOException e) {
            try {
                close();
            } catch (IOException closeException) {
                throw closeException;
            }
            throw e;
        }
    }

    @VisibleForTesting
    /* package */ VoiceHat(I2sDevice device, Max98357A dac, AudioFormat audioFormat) {
        mDevice = device;
        mDac = dac;
        mAudioFormat = audioFormat;
    }

    /* package */ AudioFormat getAudioFormat() {
        return mAudioFormat;
    }

    /**
     * Obtains the driver for the VoiceHat DAC.
     *
     * @return Driver for the DAC.
     */
    public Max98357A getDac() {
        return mDac;
    }

    public void registerAudioInputDriver() {
        if (mAudioInputDriver == null) {
            mAudioInputDriver = new AudioInputUserDriver();
            UserDriverManager.getManager().registerAudioInputDriver(mAudioInputDriver, mAudioFormat,
                AudioDeviceInfo.TYPE_BUILTIN_MIC, BUFFER_SIZE);
        } else {
            Log.w(TAG, "Audio input driver was already registered");
        }
    }

    public void registerAudioOutputDriver() {
        if (mAudioOutputDriver == null) {
            mAudioOutputDriver = new AudioOutputUserDriver();
            UserDriverManager.getManager().registerAudioOutputDriver(mAudioOutputDriver,
                mAudioFormat, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, BUFFER_SIZE);
        } else {
            Log.w(TAG, "Audio output driver was already registered");
        }
    }

    public void unregisterAudioInputDriver() {
        if (mAudioInputDriver != null) {
            UserDriverManager.getManager().unregisterAudioInputDriver(mAudioInputDriver);
            mAudioInputDriver = null;
        }
    }

    public void unregisterAudioOutputDriver() {
        if (mAudioOutputDriver != null) {
            UserDriverManager.getManager().unregisterAudioOutputDriver(mAudioOutputDriver);
            mAudioOutputDriver = null;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            mDac.close(); // Closes I2s bus
        } catch (Exception e) {
            Log.e(TAG, "Error closing DAC");
        } finally {
            mDac = null;
        }

        unregisterAudioInputDriver();
        unregisterAudioOutputDriver();
    }

    private class AudioInputUserDriver extends AudioInputDriver {
        private boolean mEnableRecord = false;

        @Override
        public void onStandbyChanged(boolean inStandby) {
            mEnableRecord = !inStandby;
        }

        @Override
        public int read(ByteBuffer byteBuffer, int i) {
            if (!mEnableRecord) {
                return 0; // Don't do any reading while not enabled.
            }
            try {
                return mDevice.read(byteBuffer, i);
            } catch (IOException e) {
                Log.e(TAG, "error during read operation:", e);
                return -1;
            }
        }
    }

    private class AudioOutputUserDriver extends AudioOutputDriver {
        private byte[] mAudioBuffer;

        @Override
        public void onStandbyChanged(boolean inStandby) {
            try {
                if (!inStandby) {
                    byte[] buf = getAudioBuffer();
                    mDac.writeI2sData(buf);
                    mDac.setSdMode(Max98357A.SD_MODE_LEFT);
                } else {
                    mDac.setSdMode(Max98357A.SD_MODE_SHUTDOWN);
                }
            } catch (IOException e) {
                Log.e(TAG, "error during standby trigger:", e);
            }
        }

        @Override
        public int write(ByteBuffer byteBuffer, int i) {
            try {
                return mDac.writeI2sData(byteBuffer, i);
            } catch (IOException e) {
                Log.e(TAG, "error during write operation:", e);
                return -1;
            }
        }

        private byte[] getAudioBuffer() {
            if (mAudioBuffer == null) {
                mAudioBuffer = new byte[FLUSH_SIZE];
            }
            return mAudioBuffer;
        }
    }
}