/*
 * Copyright 2016 Google Inc.
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
package com.google.android.things.contrib.driver.ht16k33;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.times;

import com.google.android.things.pio.I2cDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

public class Ht16k33Test {

    @Mock
    I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void setEnabled() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.setEnabled(true);
        Mockito.verify(mI2c).write(new byte[]{(byte) (0x20 | 1)}, 1);
        Mockito.verify(mI2c).write(new byte[]{(byte) (0x80 | 1)}, 1);
        driver.setEnabled(false);
        Mockito.verify(mI2c).write(new byte[]{(byte) (0x20 | 0)}, 1);
        Mockito.verify(mI2c).write(new byte[]{(byte) (0x80 | 0)}, 1);
    }

    @Test
    public void setEnabled_throwsIfClosed() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.setEnabled(true);
    }

    @Test
    public void setBrightnessInt() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        final int brightness = 5;
        driver.setBrightness(brightness);
        Mockito.verify(mI2c).write(new byte[]{(byte) (0xE0 | brightness)}, 1);
    }

    @Test
    public void setBrightnessInt_throwsIfTooSmall() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        mExpectedException.expect(IllegalArgumentException.class);
        mExpectedException.expectMessage(equalTo("brightness must be between 0 and "
                + Ht16k33.HT16K33_BRIGHTNESS_MAX));
        driver.setBrightness(-1);
    }

    @Test
    public void setBrightnessInt_throwsIfTooLarge() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        mExpectedException.expect(IllegalArgumentException.class);
        mExpectedException.expectMessage(equalTo("brightness must be between 0 and "
                + Ht16k33.HT16K33_BRIGHTNESS_MAX));
        driver.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX + 1);
    }

    @Test
    public void setBrightnessInt_throwsIfClosed() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.setBrightness(5);
    }

    @Test
    public void setBrightnessFloat() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.setBrightness(0.5f);
        int brightness = Math.round(0.5f * Ht16k33.HT16K33_BRIGHTNESS_MAX);
        Mockito.verify(mI2c).write(new byte[]{(byte) (0xE0 | brightness)}, 1);
    }

    @Test
    public void setBrightnessFloat_throwsIfTooSmall() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        mExpectedException.expect(IllegalArgumentException.class);
        driver.setBrightness(-1f);
    }

    @Test
    public void setBrightnessFloat_throwsIfTooLarge() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        mExpectedException.expect(IllegalArgumentException.class);
        driver.setBrightness(2.0f);
    }

    @Test
    public void setBrightnessFloat_throwsIfClosed() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.setBrightness(0.5f);
    }

    @Test
    public void writeColumn() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        final short data = (short) 5;
        driver.writeColumn(1, data);
        Mockito.verify(mI2c).writeRegWord(2, data);
    }

    @Test
    public void writeColumn_throwsIfClosed() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.writeColumn(1, (short) 5);
    }

    @Test
    public void writeSegments() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        short[] segments = {
                Ht16k33.SEGMENT_TOP,
                Ht16k33.SEGMENT_RIGHT_TOP,
                Ht16k33.SEGMENT_RIGHT_BOTTOM,
                Ht16k33.SEGMENT_BOTTOM,
                Ht16k33.SEGMENT_LEFT_BOTTOM,
                Ht16k33.SEGMENT_LEFT_TOP,
                Ht16k33.SEGMENT_CENTER_LEFT,
                Ht16k33.SEGMENT_CENTER_RIGHT,
                Ht16k33.SEGMENT_DIAGONAL_LEFT_TOP,
                Ht16k33.SEGMENT_CENTER_TOP,
                Ht16k33.SEGMENT_DIAGONAL_RIGHT_TOP,
                Ht16k33.SEGMENT_DIAGONAL_LEFT_BOTTOM,
                Ht16k33.SEGMENT_CENTER_BOTTOM,
                Ht16k33.SEGMENT_DIAGONAL_RIGHT_BOTTOM,
                Ht16k33.SEGMENT_DOT
        };
        // test all segments
        for (final short s: segments) {
            driver.writeColumn(0, s);
            Mockito.verify(mI2c, times(1)).writeRegWord(0, s);
        }
        // test a combination
        final short SIGMA = Ht16k33.SEGMENT_TOP | Ht16k33.SEGMENT_BOTTOM
                | Ht16k33.SEGMENT_DIAGONAL_LEFT_TOP | Ht16k33.SEGMENT_DIAGONAL_LEFT_BOTTOM;
        driver.writeColumn(0, SIGMA);
        Mockito.verify(mI2c, times(1)).writeRegWord(0, SIGMA);
    }

    @Test
    public void close() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Ht16k33 driver = new Ht16k33(mI2c);
        driver.close();
        driver.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }
}
