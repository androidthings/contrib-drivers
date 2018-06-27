/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.thermalprinter;

import android.os.Handler;
import android.util.Log;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import junit.framework.Assert;

public class MockThermalPrinter implements UartDevice {
    private static final String TAG = MockThermalPrinter.class.getSimpleName();
    private static final int PRINTER_BAUD_RATE = 19200;
    private static final int PRINTER_DATA_SIZE = 8;
    private static final int PRINTER_PARITY = UartDevice.PARITY_NONE;
    private static final int PRINTER_STOP_BITS = 1;

    public boolean mIsOpen = false;
    public List<Byte> mBytesSentList;
    public List<Byte> mBytesReadList;

    public Queue<Byte> mBytesReadBuffer;

    public MockThermalPrinter() {
        super();
        mIsOpen = true;
        mBytesSentList = new ArrayList<>();
        mBytesReadList = new ArrayList<>();

        mBytesReadBuffer = new LinkedList<>();
    }

    @Override
    public void close() throws IOException {
        mIsOpen = false;
    }

    @Override
    public String getName() {
        return MockThermalPrinter.class.getSimpleName();
    }

    @Override
    public void setBaudrate(int baudrate) throws IOException {
        Assert.assertEquals(PRINTER_BAUD_RATE, baudrate);
    }

    @Override
    public void setParity(int parity) throws IOException {
        Assert.assertEquals(PRINTER_PARITY, parity);
    }

    @Override
    public void setDataSize(int dataSize) throws IOException {
        Assert.assertEquals(PRINTER_DATA_SIZE, dataSize);
    }

    @Override
    public void setStopBits(int stopBits) throws IOException {
        Assert.assertEquals(PRINTER_STOP_BITS, stopBits);
    }

    @Override
    public int read(byte[] bytes, int size) throws IOException {
        if (mBytesReadBuffer.isEmpty()) {
            return 0;
        } else {
            for (int i = 0; i < size; i++) {
                if (!mBytesReadBuffer.isEmpty()) {
                    Log.d(TAG, "Read a byte");
                    Byte nextByte = mBytesReadBuffer.remove();
                    bytes[i] = nextByte;
                    mBytesReadList.add(nextByte);
                } else {
                    return i;
                }
            }
        }
        return size;
    }

    @Override
    public int write(byte[] bytes, int size) throws IOException {
        Log.d(TAG, "Writing " + size + " byte(s)");
        for (int i = 0; i < size; i++) {
            mBytesSentList.add(bytes[i]);
        }
        // Check if there is any data to transfer back.
        int sentSize = mBytesSentList.size();
        if (sentSize > 3 && mBytesSentList.get(sentSize - 3) == CsnA2.ASCII_ESC
            && mBytesSentList.get(sentSize - 2) == 'v') {
            Log.d(TAG, "Getting device status");
            mBytesReadBuffer.add((byte) 0x04); // No paper detected.
        }
        return size;
    }

    @Override
    public void registerUartDeviceCallback(UartDeviceCallback callback) throws IOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public void registerUartDeviceCallback(Handler handler, UartDeviceCallback uartDeviceCallback)
        throws IOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public void unregisterUartDeviceCallback(UartDeviceCallback uartDeviceCallback) {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public void setHardwareFlowControl(int i) throws IOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public void setModemControl(int i) throws IOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public void clearModemControl(int i) throws IOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public void sendBreak(int i) throws IOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public void flush(int i) throws IOException {
        // Flush!
    }
}
