package com.google.android.things.contrib.driver.ws2812b;


import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;

import com.google.android.things.contrib.driver.ws2812b.util.ColorMock;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(android.graphics.Color.class)
public class ColorChannelSequenceTest {

    private int red;
    private int green;
    private int blue;

    @Before
    public void before() {
        ColorMock.mockStatic();

        red = generateRandomColorValue();
        green = generateRandomColorValue();
        blue = generateRandomColorValue();
    }

    @Test
    public void reorderToRgb(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.RGB);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(get1stColor(rearrangedColor), red);
        Assert.assertEquals(get2ndColor(rearrangedColor), green);
        Assert.assertEquals(get3rdColor(rearrangedColor), blue);
    }

    @Test
    public void reorderToRbg(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.RBG);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(get1stColor(rearrangedColor), red);
        Assert.assertEquals(get2ndColor(rearrangedColor), blue);
        Assert.assertEquals(get3rdColor(rearrangedColor), green);
    }

    @Test
    public void reorderToGrb(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.GRB);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(get1stColor(rearrangedColor), green);
        Assert.assertEquals(get2ndColor(rearrangedColor), red);
        Assert.assertEquals(get3rdColor(rearrangedColor), blue);
    }

    @Test
    public void reorderToGbr(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.GBR);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(get1stColor(rearrangedColor), green);
        Assert.assertEquals(get2ndColor(rearrangedColor), blue);
        Assert.assertEquals(get3rdColor(rearrangedColor), red);
    }

    @Test
    public void reorderToBrg(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.BRG);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(get1stColor(rearrangedColor), blue);
        Assert.assertEquals(get2ndColor(rearrangedColor), red);
        Assert.assertEquals(get3rdColor(rearrangedColor), green);
    }

    @Test
    public void reorderToBgr(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.BGR);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(get1stColor(rearrangedColor), blue);
        Assert.assertEquals(get2ndColor(rearrangedColor), green);
        Assert.assertEquals(get3rdColor(rearrangedColor), red);
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

    private static int get3rdColor(@ColorInt int color)
    {
        return Color.blue(color);
    }
}
