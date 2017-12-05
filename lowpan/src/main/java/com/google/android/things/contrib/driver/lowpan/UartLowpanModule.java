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

package com.google.android.things.contrib.driver.lowpan;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.google.android.things.userdriver.LowpanDriverCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.android.things.userdriver.LowpanDriver.ERROR_IOFAIL;
import static com.google.android.things.userdriver.LowpanDriver.ERROR_GARBAGE;
import static com.google.android.things.userdriver.LowpanDriver.ERROR_TOOBIG;

/**
 * Peripheral that transmits spinel frames transmitted
 * over a UART.
 */
@SuppressWarnings("WeakerAccess")
public class UartLowpanModule implements AutoCloseable {

    private static final String TAG = UartLowpanModule.class.getSimpleName();

    private static final boolean DEBUG = false;
    private static final int MAX_SPINEL_SIZE = 1300;
    private final String mUartName;
    private final Handler mHandler;
    private final int mHardwareFlowControl;
    private final int mBaudRate;

    private UartDevice mDevice;
    private @NonNull LowpanDriverCallback mLowpanDriverCallback;
    private final byte[] mInboundRawBuffer = new byte[MAX_SPINEL_SIZE];
    private final byte[] mInboundFrame = new byte[MAX_SPINEL_SIZE];
    private short mInboundFrameHDLCCRC;
    private int mInboundFrameSize;
    private boolean mInboundUnescapeNextByte = false;
    private ByteBuffer mOutputBufferEscaped = ByteBuffer.allocate(MAX_SPINEL_SIZE * 2 + 6);

    private final UartDeviceCallback mUartDeviceCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            return UartLowpanModule.this.onUartDeviceDataAvailable(uart);
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            UartLowpanModule.this.onUartDeviceError(uart, error);
        }
    };

    /**
     * Create a new UartLowpanModule.
     *
     * @param lowpanDriverCallback @see {@link LowpanDriverCallback}
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     * @param hardwareFlowControl hardware flow control setting for uart device
     *        {@link UartDevice#HW_FLOW_CONTROL_NONE},
     *        {@link UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     *        @see UartDevice#HW_FLOW_CONTROL_NONE}
     *        @see UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     * @param handler optional {@link Handler} for UartDevice.
     */
    public UartLowpanModule(@NonNull LowpanDriverCallback lowpanDriverCallback, String uartName,
                            int baudRate, int hardwareFlowControl, @NonNull Handler handler) throws IOException {
        mLowpanDriverCallback = lowpanDriverCallback;
        mUartName = uartName;
        mHardwareFlowControl = hardwareFlowControl;
        mHandler = handler;
        mBaudRate = baudRate;

        final PeripheralManagerService manager = new PeripheralManagerService();

        resetFrameState();

        mDevice = manager.openUartDevice(mUartName);
        mDevice.setBaudrate(mBaudRate);
        mDevice.setDataSize(8);
        mDevice.setHardwareFlowControl(mHardwareFlowControl);
        mDevice.registerUartDeviceCallback(mUartDeviceCallback, mHandler);
    }

    /**
     * Close this device and any underlying resources associated with the connection.
     */
    @Override
    public synchronized void close() {
        if (mDevice != null) {
            mDevice.unregisterUartDeviceCallback(mUartDeviceCallback);
            mLowpanDriverCallback = null;
            try {
                mDevice.close();
            } catch (IOException x) {
                // If close fails, then we might as well rethrow and crash.
                throw new RuntimeException(x);
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Returns true if this device has been closed and is no longer usable.
     */
    public boolean isClosed() {
        return mDevice == null;
    }

    private void onError(int error) {
        mLowpanDriverCallback.onError(error);
    }

    /**
     * Sends a frame to the ncp
     *
     * @param frame byte[] frame to write to ncp
     */
    public synchronized void sendFrame(byte[] frame) throws IOException {
        if (frame.length > MAX_SPINEL_SIZE) {
            Log.e(TAG, "sendFrame: ERROR_TOOBIG, " + frame.length + " > " + MAX_SPINEL_SIZE);
            onError(ERROR_TOOBIG);
            return;
        }

        byte[] outboundFrame = escapeFrame(frame, frame.length);

        int frameSize = outboundFrame.length;
        do {
            int written = mDevice.write(outboundFrame, frameSize);
            if (frameSize > written) {
                outboundFrame = Arrays.copyOfRange(outboundFrame, written, frameSize);
                frameSize -= written;
                continue;
            }
        } while(false);
    }

    private byte[] escapeFrame(byte[] inboundFrame, int inboundFrameLen) {
        mOutputBufferEscaped.clear();

        // Write out the frame flag byte. Theoretically,
        // this isn't strictly necessary for every frame,
        // but in general the cost is low and it helps
        // things recover more quickly when they go off
        // the rails.
        mOutputBufferEscaped.put(Hdlc.HDLC_BYTE_FLAG);

        short crc = Hdlc.HDLC_CRC_RESET_VALUE;
        byte current;

        // This loop is for both CRC calculation and escaping.
        for (int i = 0; i < inboundFrameLen; i++) {
            current = inboundFrame[i];

            // Feed the byte into the CRC calculation.
            crc = Hdlc.hdlcCrc16(crc, current);

            // Escape the byte and write add it to the output buffer.
            if (Hdlc.hdlcByteNeedsEscape(current)) {
                mOutputBufferEscaped.put(Hdlc.HDLC_BYTE_ESC);
                mOutputBufferEscaped.put((byte) (current ^ Hdlc.HDLC_ESCAPE_XFORM));
            } else {
                mOutputBufferEscaped.put(current);
            }
        }

        // Finish the CRC calculation.
        crc ^= Hdlc.HDLC_CRC_RESET_VALUE;

        // Write out the first byte of the CRC(Little-endian).
        current = (byte) (crc & 0xff);
        if (Hdlc.hdlcByteNeedsEscape(current)) {
            mOutputBufferEscaped.put(Hdlc.HDLC_BYTE_ESC);
            mOutputBufferEscaped.put((byte) (current ^ Hdlc.HDLC_ESCAPE_XFORM));
        } else {
            mOutputBufferEscaped.put(current);
        }

        // Write out the second byte of the CRC(Little-endian).
        current = (byte) (crc >>> 8);
        if (Hdlc.hdlcByteNeedsEscape(current)) {
            mOutputBufferEscaped.put(Hdlc.HDLC_BYTE_ESC);
            mOutputBufferEscaped.put((byte) (current ^ Hdlc.HDLC_ESCAPE_XFORM));
        } else {
            mOutputBufferEscaped.put(current);
        }

        // Write out the frame flag byte.
        mOutputBufferEscaped.put(Hdlc.HDLC_BYTE_FLAG);

        // Return the escaped frame.
        mOutputBufferEscaped.flip();

        // Prepare our actual outbound frame
        byte[] outboundFrame = new byte[mOutputBufferEscaped.limit()];
        mOutputBufferEscaped.get(outboundFrame);

        return outboundFrame;
    }

    /**
     * Invoked by mUartDeviceCallback when new data arrives in the UART buffer.
     */
    private synchronized boolean onUartDeviceDataAvailable(UartDevice uart) {
        try {
            int count;
            while ((count = uart.read(mInboundRawBuffer, mInboundRawBuffer.length)) > 0) {
                processBuffer(mInboundRawBuffer, count);
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to read UART data", e);
            onError(ERROR_IOFAIL);
            close();
        }
        return true;
    }

    /**
     * Invoked by mUartDeviceCallback when the UART buffer experiences an error while reading.
     */
    private synchronized void onUartDeviceError(UartDevice uart, int error) {
        Log.w(TAG, "Error receiving incoming data: " + error);
        onError(ERROR_IOFAIL);
        close();
    }

    private void processBuffer(byte[] buffer, int count) {
        for (int i = 0; i < count; i++) {
            byte current = buffer[i];

            if (current == Hdlc.HDLC_BYTE_FLAG) {
                handleFrameEnd();
                resetFrameState();
                continue;

            } else if (mInboundUnescapeNextByte) {
                current ^= Hdlc.HDLC_ESCAPE_XFORM;
                mInboundUnescapeNextByte = false;

            } else if (current == Hdlc.HDLC_BYTE_ESC) {
                mInboundUnescapeNextByte = true;
                continue;
            }

            if (mInboundFrameSize == MAX_SPINEL_SIZE) {
                Log.e(TAG, "Inbound frame too large");
                onError(ERROR_GARBAGE);
                mInboundFrameSize++;

            } else if (mInboundFrameSize < MAX_SPINEL_SIZE) {
                mInboundFrameHDLCCRC = Hdlc.hdlcCrc16(mInboundFrameHDLCCRC, current);
                mInboundFrame[mInboundFrameSize++] = current;
            }
        }
    }

    /**
     * Restart the buffer and clear frame properties
     */
    private void resetFrameState() {
        mInboundUnescapeNextByte = false;
        mInboundFrameSize = 0;
        mInboundFrameHDLCCRC = Hdlc.HDLC_CRC_RESET_VALUE;
    }

    /**
     * Parse a message once the frame end character is detected.
     */
    private void handleFrameEnd() {
        if (mInboundFrameSize <= 2) {
            return;
        }

        if (mInboundFrameHDLCCRC != Hdlc.HDLC_CRC_CHECK_VALUE) {
            Log.e(TAG, String.format("Frame CRC Mismatch: Calc:0x%04X, Expected:0x%04X",
                    mInboundFrameHDLCCRC, Hdlc.HDLC_CRC_CHECK_VALUE));
            onError(ERROR_GARBAGE);
            return;
        }

        mLowpanDriverCallback.onReceiveFrame(Arrays.copyOf(mInboundFrame, mInboundFrameSize-2));
    }
}
