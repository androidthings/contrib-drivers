package com.google.android.things.contrib.driver.ws2812b;


import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.google.android.things.contrib.driver.ws2812b.util.ColorUtil;
import com.google.android.things.contrib.driver.ws2812b.util.ReverseBitPatternConverter;
import com.google.android.things.contrib.driver.ws2812b.util.ColorMock;
import com.google.android.things.contrib.driver.ws2812b.util.SparseArrayMockCreator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(android.graphics.Color.class)
public class ColorToBitPatternConverterTest {

    private final ReverseBitPatternConverter reverseConverter = new ReverseBitPatternConverter();

    @Test
    public void convertToBitPattern_Rgb() {
        ColorMock.mockStatic();
        @ColorInt int randomColor = generateRandomColor();
        ColorToBitPatternConverter converter = createColorToBitPatternConverter(ColorChannelSequence.RGB);
        byte[] bitPatterns = converter.convertToBitPattern(new int[]{randomColor});
        int reconstructedColor = reverseConverter.convertBitPatternTo24BitInt(bitPatterns);
        Assert.assertEquals(reconstructedColor, randomColor);
    }

    @Test
    public void convertToBitPattern_Rbg() {
        ColorMock.mockStatic();
        @ColorInt int randomColor = generateRandomColor();
        ColorToBitPatternConverter converter = createColorToBitPatternConverter(ColorChannelSequence.RBG);
        byte[] bitPatterns = converter.convertToBitPattern(new int[]{randomColor});
        int reconstructedColor = reverseConverter.convertBitPatternTo24BitInt(bitPatterns);
        Assert.assertEquals(ColorUtil.get1stColor(reconstructedColor), Color.red(randomColor));
        Assert.assertEquals(ColorUtil.get2ndColor(reconstructedColor), Color.blue(randomColor));
        Assert.assertEquals(ColorUtil.get3rdColor(reconstructedColor), Color.green(randomColor));
    }


    @Test
    public void convertToBitPattern_Grb() {
        ColorMock.mockStatic();
        @ColorInt int randomColor = generateRandomColor();
        ColorToBitPatternConverter converter = createColorToBitPatternConverter(ColorChannelSequence.GRB);
        byte[] bitPatterns = converter.convertToBitPattern(new int[]{randomColor});
        int reconstructedColor = reverseConverter.convertBitPatternTo24BitInt(bitPatterns);
        Assert.assertEquals(ColorUtil.get1stColor(reconstructedColor), Color.green(randomColor));
        Assert.assertEquals(ColorUtil.get2ndColor(reconstructedColor), Color.red(randomColor));
        Assert.assertEquals(ColorUtil.get3rdColor(reconstructedColor), Color.blue(randomColor));
    }

    @Test
    public void convertToBitPattern_Gbr() {
        ColorMock.mockStatic();
        @ColorInt int randomColor = generateRandomColor();
        ColorToBitPatternConverter converter = createColorToBitPatternConverter(ColorChannelSequence.GBR);
        byte[] bitPatterns = converter.convertToBitPattern(new int[]{randomColor});
        int reconstructedColor = reverseConverter.convertBitPatternTo24BitInt(bitPatterns);
        Assert.assertEquals(ColorUtil.get1stColor(reconstructedColor), Color.green(randomColor));
        Assert.assertEquals(ColorUtil.get2ndColor(reconstructedColor), Color.blue(randomColor));
        Assert.assertEquals(ColorUtil.get3rdColor(reconstructedColor), Color.red(randomColor));
    }

    @Test
    public void convertToBitPattern_Brg() {
        ColorMock.mockStatic();
        @ColorInt int randomColor = generateRandomColor();
        ColorToBitPatternConverter converter = createColorToBitPatternConverter(ColorChannelSequence.BRG);
        byte[] bitPatterns = converter.convertToBitPattern(new int[]{randomColor});
        int reconstructedColor = reverseConverter.convertBitPatternTo24BitInt(bitPatterns);
        Assert.assertEquals(ColorUtil.get1stColor(reconstructedColor), Color.blue(randomColor));
        Assert.assertEquals(ColorUtil.get2ndColor(reconstructedColor), Color.red(randomColor));
        Assert.assertEquals(ColorUtil.get3rdColor(reconstructedColor), Color.green(randomColor));
    }

    @Test
    public void convertToBitPattern_Bgr() {
        ColorMock.mockStatic();
        @ColorInt int randomColor = generateRandomColor();
        ColorToBitPatternConverter converter = createColorToBitPatternConverter(ColorChannelSequence.BGR);
        byte[] bitPatterns = converter.convertToBitPattern(new int[]{randomColor});
        int reconstructedColor = reverseConverter.convertBitPatternTo24BitInt(bitPatterns);
        Assert.assertEquals(ColorUtil.get1stColor(reconstructedColor), Color.blue(randomColor));
        Assert.assertEquals(ColorUtil.get2ndColor(reconstructedColor), Color.green(randomColor));
        Assert.assertEquals(ColorUtil.get3rdColor(reconstructedColor), Color.red(randomColor));
    }

    @ColorInt
    private int generateRandomColor() {
        return Color.rgb((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
    }

    @NonNull
    private ColorToBitPatternConverter createColorToBitPatternConverter(@ColorChannelSequence.Sequence int sequence) {
        SparseArray<byte[]> mockedSparseArray = SparseArrayMockCreator.createMockedSparseArray();
        TwelveBitIntToBitPatternMapper bitPatternMapper = new TwelveBitIntToBitPatternMapper(mockedSparseArray);
        return new ColorToBitPatternConverter(sequence, bitPatternMapper);
    }
}
