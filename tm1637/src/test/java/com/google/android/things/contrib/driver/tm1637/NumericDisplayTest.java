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

import android.text.TextUtils;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.eq;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class NumericDisplayTest {

    @Mock
    I2cBitBangDevice mDevice;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void setColonEnabled() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        final byte zero = Font.DATA[0];
        final byte zeroColon = (byte) (zero | Font.COLON);
        final byte[] expected = new byte[] {zero, zeroColon, zero, zero};

        display.setColonEnabled(true);
        assertTrue(display.getColonEnabled());
        display.display("0000");
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0), aryEq(expected), eq(4));

        Mockito.reset(mDevice);

        expected[1] = zero;
        display.setColonEnabled(false);
        assertFalse(display.getColonEnabled());
        display.display("0000");
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0), aryEq(expected), eq(4));
    }

    @Test
    public void clear() throws IOException {
        NumericDisplay display = new NumericDisplay(mDevice);
        display.clear();
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0), aryEq(new byte[]{0,0,0,0}), eq(4));
    }

    @Test
    public void clear_throwsIfClosed() throws IOException {
        NumericDisplay display = new NumericDisplay(mDevice);
        display.close();
        mExpectedException.expect(IllegalStateException.class);
        display.clear();
    }

    @Test
    public void displayInt() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.display(1234);
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0),
                aryEq(new byte[]{Font.DATA[1], Font.DATA[2], Font.DATA[3], Font.DATA[4]}), eq(4));
    }

    @Test
    public void displayInt_pads() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.display(-5);
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0),
                aryEq(new byte[]{0, 0, Font.HYPHEN, Font.DATA[5]}), eq(4));
    }

    @Test
    public void displayInt_truncates() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.display(8675309);
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0),
                aryEq(new byte[]{Font.DATA[8], Font.DATA[6], Font.DATA[7], Font.DATA[5]}), eq(4));
    }

    @Test
    public void displayInt_throwsIfClosed() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.close();
        mExpectedException.expect(IllegalStateException.class);
        display.clear();
    }

    @Test
    public void displayString() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.display("4321");
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0),
                aryEq(new byte[]{Font.DATA[4], Font.DATA[3], Font.DATA[2], Font.DATA[1]}), eq(4));
    }

    @Test
    public void displayString_spacesOk() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.display("4  1");
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0),
                aryEq(new byte[]{Font.DATA[4], 0, 0, Font.DATA[1]}), eq(4));
    }

    @Test
    public void displayString_hyphensOk() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.display("----");
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0),
                aryEq(new byte[]{Font.HYPHEN, Font.HYPHEN, Font.HYPHEN, Font.HYPHEN}), eq(4));
    }

    @Test
    public void displayString_clearsIfNullOrEmpty() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.display("7777");

        display.display(null);
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0), aryEq(new byte[]{0,0,0,0}), eq(4));

        Mockito.reset(mDevice);

        display.display("");
        Mockito.verify(mDevice).writeRegBuffer(eq(0xc0), aryEq(new byte[]{0,0,0,0}), eq(4));
    }

    @Test
    public void displayString_throwsIfClosed() throws IOException {
        TextUtilsMock.mockStatic();
        NumericDisplay display = new NumericDisplay(mDevice);
        display.close();
        mExpectedException.expect(IllegalStateException.class);
        display.display("0123");
    }
}
