/*
 * Copyright 2016 Google Inc.
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

package com.google.android.things.contrib.driver.sensehat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 *  Driver for the SenseHat LED matrix.
 */
public class LedMatrix implements AutoCloseable {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;
    private static final int BUFFER_SIZE = WIDTH * HEIGHT * 3 + 1;
    private byte[] mBuffer = new byte[BUFFER_SIZE];

    private I2cDevice mDevice;

    /**
     * Create a new LED matrix driver connected on the given I2C bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public LedMatrix(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        mDevice = pioService.openI2cDevice(bus, SenseHat.I2C_ADDRESS);
    }

    /* package */ LedMatrix(I2cDevice device) {
        mDevice = device;
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Draw the given color to the LED matrix.
     * @param color Color to draw
     * @throws IOException
     */
    public void draw(int color) throws IOException {
        mBuffer[0] = 0;
        float a = Color.alpha(color) / 255.f;
        byte r = (byte)((int)(Color.red(color)*a)>>3);
        byte g = (byte)((int)(Color.green(color)*a)>>3);
        byte b = (byte)((int)(Color.blue(color)*a)>>3);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                mBuffer[1+x+WIDTH*0+3*WIDTH*y] = r;
                mBuffer[1+x+WIDTH*1+3*WIDTH*y] = g;
                mBuffer[1+x+WIDTH*2+3*WIDTH*y] = b;
            }
        }
        mDevice.write(mBuffer, mBuffer.length);
    }

    /**
     * Draw the given drawable to the LED matrix.
     * @param drawable Drawable to draw
     * @throws IOException
     */
    public void draw(Drawable drawable) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, WIDTH, HEIGHT);
        drawable.draw(canvas);
        draw(bitmap);
    }

    /**
     * Draw the given bitmap to the LED matrix.
     * @param bitmap Bitmap to draw
     * @throws IOException
     */
    public void draw(Bitmap bitmap) throws IOException {
        Bitmap dest = Bitmap.createScaledBitmap(bitmap, 8, 8, true);
        mBuffer[0] = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int p = bitmap.getPixel(x, y);
                float a = Color.alpha(p) / 255.f;
                mBuffer[1+x+WIDTH*0+3*WIDTH*y] = (byte)((int)(Color.red(p)*a)>>3);
                mBuffer[1+x+WIDTH*1+3*WIDTH*y] = (byte)((int)(Color.green(p)*a)>>3);
                mBuffer[1+x+WIDTH*2+3*WIDTH*y] = (byte)((int)(Color.blue(p)*a)>>3);
            }
        }
        mDevice.write(mBuffer, mBuffer.length);
    }
}
