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

package com.google.android.things.contrib.driver.pwmspeaker;

import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;

import java.io.IOException;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Speaker implements AutoCloseable {

    private Pwm mPwm;

    /**
     * Create a Speaker connected to the given PWM pin name
     */
    public Speaker(String pin) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Pwm device = pioService.openPwm(pin);
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
     * Create a Speaker from a {@link Pwm} device
     */
    @VisibleForTesting
    /*package*/ Speaker(Pwm device) throws IOException {
        connect(device);
    }

    private void connect(Pwm device) throws IOException {
        mPwm = device;
        mPwm.setPwmDutyCycle(50.0); // square wave
    }

    @Override
    public void close() throws IOException {
        if (mPwm != null) {
            try {
                mPwm.close();
            } finally {
                mPwm = null;
            }
        }
    }

    /**
     * Play the specified frequency. Play continues until {@link #stop()} is called.
     *
     * @param frequency the frequency to play in Hz
     * @throws IOException
     * @throws IllegalStateException if the device is closed
     */
    public void play(double frequency) throws IOException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.setPwmFrequencyHz(frequency);
        mPwm.setEnabled(true);
    }

    /**
     * Stop a currently playing frequency
     *
     * @throws IOException
     * @throws IllegalStateException if the device is closed
     */
    public void stop() throws IOException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.setEnabled(false);
    }
}
