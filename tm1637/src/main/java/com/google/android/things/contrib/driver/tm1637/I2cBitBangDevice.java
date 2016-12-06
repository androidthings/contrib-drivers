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

// Ported from https://github.com/intel-iot-devkit/upm/tree/master/src/tm1637
/*
 * Author: Mihai Tudor Panu <mihai.tudor.panu@intel.com>
 * Copyright (c) 2015 Intel Corporation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.google.android.things.contrib.driver.tm1637;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.Closeable;
import java.io.IOException;

class I2cBitBangDevice implements Closeable {
    private int mAddress;
    private Gpio mData;
    private Gpio mClock;

    public I2cBitBangDevice(int i2cAddress, String pinData, String pinClock) throws IOException {
        mAddress = i2cAddress;
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mData = pioService.openGpio(pinData);
            mData.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mClock = pioService.openGpio(pinClock);
            mClock.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (mData != null) {
            try {
                mData.close();
            } finally {
                mData = null;
            }
        }
        if (mClock != null) {
            try {
                mClock.close();
            } finally {
                mClock = null;
            }
        }
    }

    public void write(byte[] buffer, int size) throws IOException {
        start();
        writeByte(mAddress);
        stop();
        start();
        for (int i = 0; i < size; i++) {
            writeByte(buffer[i]);
        }
        stop();
    }

    public void writeRegBuffer(int reg, byte[] buffer, int size) throws IOException {
        start();
        writeByte(mAddress);
        stop();
        start();
        writeByte(reg);
        for (int i = 0; i < size; i++) {
            writeByte(buffer[i]);
        }
        stop();
    }

    void start() throws IOException {
        mClock.setValue(true);
        mData.setValue(true);
        mData.setValue(false);
    }

    void writeByte(int data) throws IOException {
        for (int i = 0; i < 8; i++) {
            mClock.setValue(false);
            mData.setValue((data & (1 << i)) != 0);
            mClock.setValue(true);
        }
        mClock.setValue(false);
        mClock.setValue(true);
        mData.setValue(false);
    }

    void stop() throws IOException {
        mClock.setValue(false);
        mData.setValue(false);
        mClock.setValue(true);
        mData.setValue(true);
    }
}
