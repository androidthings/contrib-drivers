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

package com.google.android.things.contrib.driver.gps;

import android.os.Handler;

import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.nio.ByteBuffer;

/**
 * Fake UART device implementation that locally caches data
 * written to it, and returns the same buffer back to any
 * read requests.
 */
public class LoopbackUartDevice implements UartDevice {
    private UartDeviceCallback mCallback;
    private ByteBuffer mDataBuffer;

    @Override
    public void close() {
        // Do nothing...not a real device
    }

    @Override
    public void setBaudrate(int rate) {
        // Do nothing...not a real device
    }

    @Override
    public void setParity(int mode) {
        // Do nothing...not a real device
    }

    @Override
    public void setDataSize(int size) {
        // Do nothing...not a real device
    }

    @Override
    public void setStopBits(int bits) {
        // Do nothing...not a real device
    }

    @Override
    public void setHardwareFlowControl(int mode) {
        // Do nothing...not a real device
    }

    @Override
    public void setModemControl(int lines) {
        // Do nothing...not a real device
    }

    @Override
    public void clearModemControl(int lines) {
        // Do nothing...not a real device
    }

    @Override
    public void sendBreak(int duration) {
        // Do nothing...not a real device
    }

    @Override
    public void flush(int direction) {
        // Do nothing...not a real device
    }

    @Override
    public int read(byte[] buffer, int length) {
        int readLength = (mDataBuffer.remaining() < length) ? mDataBuffer.remaining() : length;
        mDataBuffer.get(buffer, 0, readLength);
        return readLength;
    }

    @Override
    public int write(byte[] buffer, int length) {
        mDataBuffer = ByteBuffer.wrap(buffer);
        mCallback.onUartDeviceDataAvailable(LoopbackUartDevice.this);
        return length;
    }

    @Override
    public void registerUartDeviceCallback(Handler handler, UartDeviceCallback callback) {
        mCallback = callback;
    }

    @Override
    public void unregisterUartDeviceCallback(UartDeviceCallback callback) {
        mCallback = null;
    }
}
