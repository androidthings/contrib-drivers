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
import android.util.Log;

import com.google.android.things.contrib.driver.hts221.Hts221;
import com.google.android.things.contrib.driver.lps25h.Lps25h;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SenseHatDeviceTest {

    @Test
    public void senseHat_DisplayColor() throws IOException {
        // Color the LED matrix.
        SenseHat senseHat = new SenseHat();
        LedMatrix display = senseHat.openDisplay();
        display.draw(Color.MAGENTA);

        // Close the display when done.
        senseHat.close();
    }

    @Test
    public void senseHat_DisplayDrawable() throws IOException {
        Context context = InstrumentationRegistry.getTargetContext();

        // Display a drawable on the LED matrix.
        SenseHat senseHat = new SenseHat();
        LedMatrix display = senseHat.openDisplay();
        display.draw(context.getDrawable(android.R.drawable.ic_secure));

        // Close the display when done.
        senseHat.close();
    }

    @Test
    public void senseHat_DisplayGradient() throws IOException {
        // Display a gradient on the LED matrix.
        SenseHat senseHat = new SenseHat();
        LedMatrix display = senseHat.openDisplay();
        Bitmap bitmap = Bitmap.createBitmap(SenseHat.DISPLAY_WIDTH, SenseHat.DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setShader(new RadialGradient(4, 4, 4, Color.RED, Color.BLUE, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        display.draw(bitmap);

        // Close the display when done.
        senseHat.close();
    }

    @Test
    public void senseHat_ReadPressureSensor() throws IOException {
        // Prints LPS25H barometric pressure and temperature sensor readings to LogCat
        Lps25h lps25h = SenseHat.openPressureSensor();
        float pressure = lps25h.readPressure();
        Log.i("LPS25H", String.format("Barometric Pressure: %.1f", pressure) + " hPa");
        float temperature = lps25h.readTemperature();
        Log.i("LPS25H", String.format("Temperature: %.1f", temperature) + " °C");
        lps25h.close();
    }

    @Test
    public void senseHat_ReadHumiditySensor() throws IOException {
        // Prints HTS221 relative humidity and temperature sensor readings to LogCat
        Hts221 hts221 = SenseHat.openHumiditySensor();
        float humidity = hts221.readHumidity();
        Log.i("HTS221", String.format("Relative Humidity: %.1f", humidity) + " %");
        float temperature = hts221.readTemperature();
        Log.i("HTS221", String.format("Temperature: %.1f", temperature) + " °C");
        hts221.close();
    }

}
