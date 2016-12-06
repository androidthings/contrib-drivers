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

package com.google.android.things.contrib.driver.ssd1306;

import android.graphics.Bitmap;

public class BitmapHelper {
    private static final int GRADIENT_CUTOFF = 170; // Tune for gradient picker on grayscale images.

    /**
     * Converts a bitmap image to LCD screen data and sets it on the given screen at the specified
     * offset.
     * @param mScreen The OLED screen to write the bitmap data to.
     * @param xOffset The horizontal offset to draw the image at.
     * @param yOffset The vertical offset to draw the image at.
     * @param bmp The bitmap image that you want to convert to screen data.
     * @param drawWhite true for drawing only white pixels, false for drawing grayscale pixel
     * based on {@link #GRADIENT_CUTOFF}.
     */
    public static void setBmpData(Ssd1306 mScreen, int xOffset, int yOffset, Bitmap bmp,
            boolean drawWhite) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int bmpByteSize = (int) Math.ceil((double) (width * ((height / 8) > 1 ? (height / 8) : 1)));

        // Each byte stored in memory represents 8 vertical pixels.  As such, you must fill the
        // memory with pixel data moving vertically top-down through the image and scrolling
        // across, while appending the vertical pixel data by series of 8.

        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x++) {
                int bytePos = x + ((y / 8) * width);

                for (int k = 0; k < 8; k++) {
                    if ((k + y < height) && (bytePos < bmpByteSize)) {
                        int pixel = bmp.getPixel(x, y + k);
                        if (!drawWhite) { // Look at Alpha channel instead
                            if ((pixel & 0xFF) > GRADIENT_CUTOFF) {
                                mScreen.setPixel(x + xOffset, y + yOffset + k, true);
                            }
                        } else {
                            if (pixel == -1) { // Only draw white pixels
                                mScreen.setPixel(x + xOffset, y + yOffset + k, true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts a bitmap image to LCD screen data and returns the screen data as bytes.
     * @param buffer The screen's data buffer.
     * @param offset The byte offset to start writing screen bitmap data at.
     * @param bmp The bitmap image that you want to convert to screen data.
     * @param drawWhite Set to true to draw white pixels, false to draw pixels based on gradient.
     * @return A byte array with pixel data for the SSD1306.
     */
    public static void bmpToBytes(byte[] buffer, int offset, Bitmap bmp, boolean drawWhite) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        // Each byte stored in memory represents 8 vertical pixels.  As such, you must fill the
        // memory with pixel data moving vertically top-down through the image and scrolling
        // across, while appending the vertical pixel data by series of 8.
        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x++) {
                int bytePos = (offset + x) + ((y / 8) * width);

                for (int k = 0; k < 8; k++) {
                    if ((k + y < height) && (bytePos < buffer.length)) {
                        int pixel = bmp.getPixel(x, y + k);
                        if (!drawWhite) { // Look at Alpha channel instead
                            if ((pixel & 0xFF) > GRADIENT_CUTOFF) {
                                buffer[bytePos] |= 1 << k;
                            }
                        } else {
                            if (pixel == -1) { // Only draw white pixels
                                buffer[bytePos] |= 1 << k;
                            }
                        }
                    }
                }
            }
        }
    }
}
