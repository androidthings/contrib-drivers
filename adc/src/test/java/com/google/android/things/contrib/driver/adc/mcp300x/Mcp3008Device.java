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

package com.google.android.things.contrib.driver.adc.mcp300x;

import com.google.android.things.pio.SpiDevice;

/**
 * Mock instance of an MCP3008 ADC
 * <p>
 * Responses modeled after MCU interface spec in datasheet:
 * https://cdn-shop.adafruit.com/datasheets/MCP3008.pdf
 */
public class Mcp3008Device implements SpiDevice {

    private static final byte FLAG_INPUT_TYPE = (byte) 0x80;
    private static final byte MASK_CHANNEL = 0x70;

    /**
     * Channel input pairs for the various differential modes.
     */
    private enum DifferentialMode {
        MODE0(0,1),
        MODE1(1,0),
        MODE2(2,3),
        MODE3(3,2),
        MODE4(4,5),
        MODE5(5,4),
        MODE6(6,7),
        MODE7(7,6);

        final int positive;
        final int negative;
        DifferentialMode(int positive, int negative) {
            this.positive = positive;
            this.negative = negative;
        }
    }

    private int[] mCurrentValues = new int[8];

    /**
     * Update the internal mock ADC values for each channel
     *
     * @param values
     */
    public void setChannelValues(int... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] < 0 || values[i] > 1023) {
                throw new IllegalArgumentException("Value " + i + " must be a valid 10-bit result");
            }

            if (i < mCurrentValues.length) {
                mCurrentValues[i] = values[i];
            }
        }
    }

    @Override
    public void close() {
        // Do nothing...not a real device
    }

    @Override
    public void setFrequency(int frequencyHz) {
        if (frequencyHz > 1000000) {
            throw new UnsupportedOperationException("MCP3008 is not stable above 1MHz");
        }
    }

    @Override
    public void setBitJustification(int justification) {
        if (justification != BIT_JUSTIFICATION_MSB_FIRST) {
            throw new UnsupportedOperationException("MCP3008 requires MSB first mode");
        }
    }

    @Override
    public void setDelay(int delayUs) {
        // Do nothing...not a real device
    }

    @Override
    public void setMode(int mode) {
        if (mode != MODE0) {
            throw new UnsupportedOperationException("MCP3008 requires MODE0");
        }
    }

    @Override
    public void setBitsPerWord(int bitsPerWord) {
        // Do nothing...not a real device
    }

    @Override
    public void setCsChange(boolean change) {
        // Do nothing...not a real device
    }

    @Override
    public void transfer(byte[] txBuffer, byte[] rxBuffer, int length) {
        if (txBuffer == null) {
            throw new UnsupportedOperationException("MCP3008 does not support read-only");
        }

        if (rxBuffer == null) {
            // Ignore transmit only commands
            return;
        }

        if (txBuffer.length != rxBuffer.length) {
            throw new IllegalStateException("Buffer sizes must match");
        }

        if (length != 3) {
            throw new UnsupportedOperationException("Invalid MCP3008 command");
        }

        // Parse incoming command
        boolean singleEnded = (txBuffer[1] & FLAG_INPUT_TYPE) == FLAG_INPUT_TYPE;
        int channelMode = (txBuffer[1] & MASK_CHANNEL) >>> 4;

        int returnValue;
        if (singleEnded) {
            returnValue = mCurrentValues[channelMode];
        } else {
            DifferentialMode mode = DifferentialMode.values()[channelMode];
            returnValue = mCurrentValues[mode.positive]
                    - mCurrentValues[mode.negative];
            // MCP3008 will return 0 if differential is negative
            returnValue = Math.max(returnValue, 0);
        }

        // Set outgoing data
        rxBuffer[0] = (byte) 0xFF;
        rxBuffer[1] = (byte) (0xF8 + ((returnValue >> 8) & 0x03));
        rxBuffer[2] = (byte) (returnValue & 0xFF);
    }

}
