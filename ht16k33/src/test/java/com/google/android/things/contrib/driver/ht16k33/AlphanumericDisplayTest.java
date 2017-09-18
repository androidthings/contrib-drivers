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

import android.text.TextUtils;

import com.google.android.things.pio.I2cDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class AlphanumericDisplayTest {

    @Mock
    I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void clear() throws IOException {
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.clear();
        Mockito.verify(mI2c).writeRegWord(0, (short) 0);
        Mockito.verify(mI2c).writeRegWord(2, (short) 0);
        Mockito.verify(mI2c).writeRegWord(4, (short) 0);
        Mockito.verify(mI2c).writeRegWord(6, (short) 0);
    }

    @Test
    public void clear_throwsIfClosed() throws IOException {
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.close();
        mExpectedException.expect(IllegalStateException.class);
        display.clear();
    }

    @Test
    public void displayInt() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display(1234);
        Mockito.verify(mI2c).writeRegWord(0, (short) Font.DATA['1']);
        Mockito.verify(mI2c).writeRegWord(2, (short) Font.DATA['2']);
        Mockito.verify(mI2c).writeRegWord(4, (short) Font.DATA['3']);
        Mockito.verify(mI2c).writeRegWord(6, (short) Font.DATA['4']);
    }

    @Test
    public void displayInt_pads() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display(7);
        Mockito.verify(mI2c).writeRegWord(0, (short) 0);
        Mockito.verify(mI2c).writeRegWord(2, (short) 0);
        Mockito.verify(mI2c).writeRegWord(4, (short) 0);
        Mockito.verify(mI2c).writeRegWord(6, (short) Font.DATA['7']);
    }

    @Test
    public void displayInt_truncates() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display(8675309);
        Mockito.verify(mI2c).writeRegWord(0, (short) Font.DATA['8']);
        Mockito.verify(mI2c).writeRegWord(2, (short) Font.DATA['6']);
        Mockito.verify(mI2c).writeRegWord(4, (short) Font.DATA['7']);
        Mockito.verify(mI2c).writeRegWord(6, (short) Font.DATA['5']);
    }

    @Test
    public void displayInt_throwsIfClosed() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.close();
        mExpectedException.expect(IllegalStateException.class);
        display.display(0);
    }

    @Test
    public void displayDouble() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display(11.22);
        Mockito.verify(mI2c).writeRegWord(0, (short) Font.DATA['1']);
        Mockito.verify(mI2c).writeRegWord(2, (short) (Font.DATA['1'] | Font.DATA['.']));
        Mockito.verify(mI2c).writeRegWord(4, (short) Font.DATA['2']);
        Mockito.verify(mI2c).writeRegWord(6, (short) Font.DATA['2']);
    }

    @Test
    public void displayDouble_pads() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display(0.5);
        Mockito.verify(mI2c).writeRegWord(0, (short) 0);
        Mockito.verify(mI2c).writeRegWord(2, (short) 0);
        Mockito.verify(mI2c).writeRegWord(4, (short) (Font.DATA['0'] | Font.DATA['.']));
        Mockito.verify(mI2c).writeRegWord(6, (short) Font.DATA['5']);
    }

    @Test
    public void displayDouble_truncates() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display(86.75309);
        Mockito.verify(mI2c).writeRegWord(0, (short) Font.DATA['8']);
        Mockito.verify(mI2c).writeRegWord(2, (short) (Font.DATA['6'] | Font.DATA['.']));
        Mockito.verify(mI2c).writeRegWord(4, (short) Font.DATA['7']);
        Mockito.verify(mI2c).writeRegWord(6, (short) Font.DATA['5']);
    }

    @Test
    public void displayDouble_throwsIfClosed() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.close();
        mExpectedException.expect(IllegalStateException.class);
        display.display(0);
    }

    @Test
    public void displayString() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display("foo");
        Mockito.verify(mI2c).writeRegWord(0, (short) Font.DATA['f']);
        Mockito.verify(mI2c).writeRegWord(2, (short) Font.DATA['o']);
        Mockito.verify(mI2c).writeRegWord(4, (short) Font.DATA['o']);
        Mockito.verify(mI2c).writeRegWord(6, (short) 0); // make sure it clears remaining columns
    }

    @Test
    public void displayString_consecutiveDotsOk() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display("..");
        Mockito.verify(mI2c).writeRegWord(0, (short) Font.DATA['.']);
        Mockito.verify(mI2c).writeRegWord(2, (short) Font.DATA['.']);
        Mockito.verify(mI2c).writeRegWord(4, (short) 0);
        Mockito.verify(mI2c).writeRegWord(6, (short) 0);
    }

    @Test
    public void displayString_appendDotsOk() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.display("E.T.L.A.");
        Mockito.verify(mI2c).writeRegWord(0, (short) (Font.DATA['E'] | Font.DATA['.']));
        Mockito.verify(mI2c).writeRegWord(2, (short) (Font.DATA['T'] | Font.DATA['.']));
        Mockito.verify(mI2c).writeRegWord(4, (short) (Font.DATA['L'] | Font.DATA['.']));
        Mockito.verify(mI2c).writeRegWord(6, (short) (Font.DATA['A'] | Font.DATA['.']));
    }

    @Test
    public void displayString_clearsIfNullOrEmpty() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);

        display.display(null);
        Mockito.verify(mI2c).writeRegWord(0, (short) 0);
        Mockito.verify(mI2c).writeRegWord(2, (short) 0);
        Mockito.verify(mI2c).writeRegWord(4, (short) 0);
        Mockito.verify(mI2c).writeRegWord(6, (short) 0);

        Mockito.reset(mI2c);

        display.display("");
        Mockito.verify(mI2c).writeRegWord(0, (short) 0);
        Mockito.verify(mI2c).writeRegWord(2, (short) 0);
        Mockito.verify(mI2c).writeRegWord(4, (short) 0);
        Mockito.verify(mI2c).writeRegWord(6, (short) 0);
    }

    @Test
    public void displayString_throwsIfClosed() throws IOException {
        TextUtilsMock.mockStatic();
        AlphanumericDisplay display = new AlphanumericDisplay(mI2c);
        display.close();
        mExpectedException.expect(IllegalStateException.class);
        display.display("foo");
    }
}
