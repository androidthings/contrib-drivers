package com.google.android.things.contrib.driver.ws2812b;


import android.graphics.Color;

import com.google.android.things.contrib.driver.ws2812b.util.ColorMock;
import com.google.android.things.contrib.driver.ws2812b.util.ColorUtil;

import org.junit.Assert;
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
    public void setUp() {
        ColorMock.mockStatic();

        red = ColorUtil.generateRandomColorValue();
        green = ColorUtil.generateRandomColorValue();
        blue = ColorUtil.generateRandomColorValue();
    }

    @Test
    public void reorderToRgb(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.RGB);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(ColorUtil.get1stColor(rearrangedColor), red);
        Assert.assertEquals(ColorUtil.get2ndColor(rearrangedColor), green);
        Assert.assertEquals(ColorUtil.get3rdColor(rearrangedColor), blue);
    }

    @Test
    public void reorderToRbg(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.RBG);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(ColorUtil.get1stColor(rearrangedColor), red);
        Assert.assertEquals(ColorUtil.get2ndColor(rearrangedColor), blue);
        Assert.assertEquals(ColorUtil.get3rdColor(rearrangedColor), green);
    }

    @Test
    public void reorderToGrb(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.GRB);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(ColorUtil.get1stColor(rearrangedColor), green);
        Assert.assertEquals(ColorUtil.get2ndColor(rearrangedColor), red);
        Assert.assertEquals(ColorUtil.get3rdColor(rearrangedColor), blue);
    }

    @Test
    public void reorderToGbr(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.GBR);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(ColorUtil.get1stColor(rearrangedColor), green);
        Assert.assertEquals(ColorUtil.get2ndColor(rearrangedColor), blue);
        Assert.assertEquals(ColorUtil.get3rdColor(rearrangedColor), red);
    }

    @Test
    public void reorderToBrg(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.BRG);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(ColorUtil.get1stColor(rearrangedColor), blue);
        Assert.assertEquals(ColorUtil.get2ndColor(rearrangedColor), red);
        Assert.assertEquals(ColorUtil.get3rdColor(rearrangedColor), green);
    }

    @Test
    public void reorderToBgr(){
        ColorChannelSequence.Sequencer sequencer = ColorChannelSequence.createSequencer(ColorChannelSequence.BGR);
        int rgbColor = Color.rgb(red, green, blue);
        int rearrangedColor = sequencer.rearrangeColorChannels(rgbColor);

        Assert.assertEquals(ColorUtil.get1stColor(rearrangedColor), blue);
        Assert.assertEquals(ColorUtil.get2ndColor(rearrangedColor), green);
        Assert.assertEquals(ColorUtil.get3rdColor(rearrangedColor), red);
    }

}
