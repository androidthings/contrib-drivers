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
package com.google.android.things.contrib.driver.tm1637;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

public class Tm1637Test {

    @Mock
    I2cBitBangDevice mDevice;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void setBrightnessInt() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        final int brightness = 3;
        tm1637.setBrightness(brightness);
        Mockito.verify(mDevice).write(aryEq(new byte[] {(byte) (0x88 | brightness)}), eq(1));
    }

    @Test
    public void setBrightnessInt_throwsIfTooSmall() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        mExpectedException.expect(IllegalArgumentException.class);
        tm1637.setBrightness(-1);
    }

    @Test
    public void setBrightnessInt_throwsIfTooLarge() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        mExpectedException.expect(IllegalArgumentException.class);
        tm1637.setBrightness(Tm1637.MAX_BRIGHTNESS + 1);
    }

    @Test
    public void setBrightnessInt_throwsIfClosed() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        tm1637.close();
        mExpectedException.expect(IllegalStateException.class);
        tm1637.setBrightness(1);
    }

    @Test
    public void setBrightnessFloat() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        final float brightness = 0.5f;
        tm1637.setBrightness(brightness);
        final int expected = Math.round(brightness * Tm1637.MAX_BRIGHTNESS);
        Mockito.verify(mDevice).write(aryEq(new byte[] {(byte) (0x88 | expected)}), eq(1));
    }

    @Test
    public void setBrightnessFloat_throwsIfTooSmall() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        mExpectedException.expect(IllegalArgumentException.class);
        tm1637.setBrightness(-1f);
    }

    @Test
    public void setBrightnessFloat_throwsIfTooLarge() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        mExpectedException.expect(IllegalArgumentException.class);
        tm1637.setBrightness(2f);
    }

    @Test
    public void setBrightnessFloat_throwsIfClosed() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        tm1637.close();
        mExpectedException.expect(IllegalStateException.class);
        tm1637.setBrightness(0.5f);
    }

    @Test
    public void writeData() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        final byte[] expected = new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
        tm1637.writeData(expected);
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0), aryEq(expected), eq(4));
    }

    @Test
    public void writeData_throwsIfArrayTooLarge() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        mExpectedException.expect(IllegalArgumentException.class);
        tm1637.writeData(new byte[Tm1637.MAX_DATA_LENGTH + 1]);
    }

    @Test
    public void writeData_throwsIfClosed() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        tm1637.close();
        mExpectedException.expect(IllegalStateException.class);
        tm1637.writeData(new byte[4]);
    }

    @Test
    public void close() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        tm1637.close();
        Mockito.verify(mDevice).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Tm1637 tm1637 = new Tm1637(mDevice);
        tm1637.close();
        tm1637.close(); // should not throw
        Mockito.verify(mDevice, times(1)).close();
    }
}
