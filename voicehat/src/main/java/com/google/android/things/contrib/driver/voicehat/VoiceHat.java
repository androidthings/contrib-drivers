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
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2sDevice;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.userdriver.AudioInputDriver;
import com.google.android.things.userdriver.AudioOutputDriver;
import com.google.android.things.userdriver.UserDriverManager;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VoiceHat implements AutoCloseable {
    private static final String TAG = VoiceHat.class.getSimpleName();

    // buffer of 0.05 sec of sample data at 48khz / 16bit.
    private static final int BUFFER_SIZE = 96000 / 20;
    // buffer of 0.5 sec of sample data at 48khz / 16bit.
    private static final int FLUSH_SIZE = 48000;

    private I2sDevice mDevice;
    private Gpio mTriggerGpio;

    private AudioFormat mAudioFormat;
    private AudioInputUserDriver mAudioInputDriver;
    private AudioOutputUserDriver mAudioOutputDriver;

    public VoiceHat(String i2sBus, String triggerGpioPin, AudioFormat audioFormat)
            throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mDevice = pioService.openI2sDevice(i2sBus, audioFormat);
            mTriggerGpio = pioService.openGpio(triggerGpioPin);
            mTriggerGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    @VisibleForTesting
    /* package */ VoiceHat(I2sDevice device, AudioFormat audioFormat) {
        mDevice = device;
        mAudioFormat = audioFormat;
    }

    public void registerAudioInputDriver() {
        mAudioInputDriver = new AudioInputUserDriver();
        UserDriverManager.getManager().registerAudioInputDriver(mAudioInputDriver, mAudioFormat,
                AudioDeviceInfo.TYPE_BUILTIN_MIC, BUFFER_SIZE);
    }

    public void registerAudioOutputDriver() {
        mAudioOutputDriver = new AudioOutputUserDriver();
        UserDriverManager.getManager().registerAudioOutputDriver(mAudioOutputDriver, mAudioFormat,
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, BUFFER_SIZE);
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
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
        if (mTriggerGpio != null) {
            try {
                mTriggerGpio.close();
            } finally {
                mTriggerGpio = null;
            }
        }

        unregisterAudioInputDriver();
        unregisterAudioOutputDriver();
    }

    private class AudioInputUserDriver extends AudioInputDriver {
        @Override
        public void onStandbyChanged(boolean b) {
        }

        @Override
        public int read(ByteBuffer byteBuffer, int i) {
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
                    mDevice.write(buf, 0, buf.length);
                    mTriggerGpio.setValue(true);
                } else {
                    mTriggerGpio.setValue(false);
                }
            } catch (IOException e) {
                Log.e(TAG, "error during standby trigger:", e);
            }
        }

        @Override
        public int write(ByteBuffer byteBuffer, int i) {
            try {
                return mDevice.write(byteBuffer, i);
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