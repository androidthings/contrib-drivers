package com.google.android.things.contrib.driver.ssd1306;

import android.graphics.Bitmap;

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

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BitmapHelper.class, Bitmap.class})
public class Ssd1306Test {

    @Mock
    I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void setContrastValidValues() throws IOException {
        mockStatic(BitmapHelper.class);
        mockStatic(Bitmap.class);
        Ssd1306 ssd1306 = new Ssd1306(mI2c);

        int[] validValues = {0, 66, 255};

        for (int validValue : validValues) {
            ssd1306.setContrast(validValue);
            Mockito.verify(mI2c).writeRegByte(0x00, (byte) 0x81);
            Mockito.verify(mI2c).writeRegByte(0x00, (byte) validValue);
            Mockito.reset(mI2c);
        }
    }

    @Test
    public void setContrastInValidValues() throws IOException {
        int[] invalidValues = {-777, -1, 256, 999};
        mockStatic(BitmapHelper.class);
        mockStatic(Bitmap.class);
        Ssd1306 ssd1306 = new Ssd1306(mI2c);

        for (int invalidValue : invalidValues) {
            mExpectedException.expect(IllegalArgumentException.class);
            mExpectedException.expectMessage("Invalid contrast " + String.valueOf(invalidValue) + ", level must be between 0 and 255");
            ssd1306.setContrast(invalidValue);
            Mockito.verify(mI2c, Mockito.never()).writeRegByte(0x00, (byte) 0x81);
            Mockito.verify(mI2c, Mockito.never()).writeRegByte(0x00, (byte) 66);
            Mockito.reset(mExpectedException);
        }
    }

    @Test
    public void nullmI2cDevice() throws IOException {
        Ssd1306 ssd1306 = mock(Ssd1306.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C Device not open");
        ssd1306.setContrast(44);
    }
}
