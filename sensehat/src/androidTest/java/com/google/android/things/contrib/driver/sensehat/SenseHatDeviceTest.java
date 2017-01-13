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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SenseHatDeviceTest {
    @Test
    public void senseHat_DisplayColor() throws IOException {
        // Color the LED matrix.
        LedMatrix display = SenseHat.openDisplay();

        display.draw(Color.MAGENTA);
        // Close the display when done.
        display.close();
    }

    @Test
    public void senseHat_DisplayDrawable() throws IOException {
        Context context = InstrumentationRegistry.getTargetContext();
        // Display a drawable on the LED matrix.
        LedMatrix display = SenseHat.openDisplay();
        display.draw(context.getDrawable(android.R.drawable.ic_secure));
        // Close the display when done.
        display.close();
    }

    @Test
    public void senseHat_DisplayGradient() throws IOException {
        // Display a gradient on the LED matrix.
        LedMatrix display = SenseHat.openDisplay();
        Bitmap bitmap = Bitmap.createBitmap(SenseHat.DISPLAY_WIDTH, SenseHat.DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setShader(new RadialGradient(4, 4, 4, Color.RED, Color.BLUE, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        display.draw(bitmap);
        // Close the display when done.
        display.close();
    }
}
