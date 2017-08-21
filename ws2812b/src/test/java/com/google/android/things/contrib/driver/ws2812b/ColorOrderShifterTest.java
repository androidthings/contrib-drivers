package com.google.android.things.contrib.driver.ws2812b;


import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(android.graphics.Color.class)
public class ColorOrderShifterTest {

    private int red;
    private int green;
    private int blue;

    @Before
    public void setUp() {
        ColorMock.mockStatic();

        red = generateRandomColorValue();
        green = generateRandomColorValue();
        blue = generateRandomColorValue();
    }

    @Test
    public void reorderToRbg(){

        int rbgColor = ColorOrderShifter.reorderColorToRbg(Color.rgb(red, green, blue));

        Assert.assertEquals(get1stColor(rbgColor), red);
        Assert.assertEquals(get2ndColor(rbgColor), blue);
        Assert.assertEquals(get3ndColor(rbgColor), green);
    }

    @Test
    public void reorderToGrb(){
        int grb = ColorOrderShifter.reorderColorToGrb(Color.rgb(red, green, blue));

        Assert.assertEquals(get1stColor(grb), green);
        Assert.assertEquals(get2ndColor(grb), red);
        Assert.assertEquals(get3ndColor(grb), blue);
    }

    @Test
    public void reorderToGbr(){
        int gbrColor = ColorOrderShifter.reorderColorToGbr(Color.rgb(red, green, blue));

        Assert.assertEquals(get1stColor(gbrColor), green);
        Assert.assertEquals(get2ndColor(gbrColor), blue);
        Assert.assertEquals(get3ndColor(gbrColor), red);
    }

    @Test
    public void reorderToBrg(){
        int brgColor = ColorOrderShifter.reorderColorToBrg(Color.rgb(red, green, blue));

        Assert.assertEquals(get1stColor(brgColor), blue);
        Assert.assertEquals(get2ndColor(brgColor), red);
        Assert.assertEquals(get3ndColor(brgColor), green);
    }

    @Test
    public void reorderToBgr(){
        int bgrColor = ColorOrderShifter.reorderColorToBgr(Color.rgb(red, green, blue));

        Assert.assertEquals(get1stColor(bgrColor), blue);
        Assert.assertEquals(get2ndColor(bgrColor), green);
        Assert.assertEquals(get3ndColor(bgrColor), red);
    }

    @IntRange(from = 0, to = 255)
    private int generateRandomColorValue() {
        return (int) Math.round(Math.random() * 255);
    }

    private static int get1stColor(@ColorInt int color)
    {
        return Color.red(color);
    }

    private static int get2ndColor(@ColorInt int color)
    {
        return Color.green(color);
    }

    private static int get3ndColor(@ColorInt int color)
    {
        return Color.blue(color);
    }
}
