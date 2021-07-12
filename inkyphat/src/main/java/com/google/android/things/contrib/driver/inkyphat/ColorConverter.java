package com.google.android.things.contrib.driver.inkyphat;

import android.graphics.Color;

class ColorConverter {

    InkyPhat.Palette convertARBG888Color(int color) {
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);

        if (red > 127 && blue > 127 && green > 127) {
            return InkyPhat.Palette.WHITE;
        }

        if (red > 127) {
            return InkyPhat.Palette.RED;
        }

        return InkyPhat.Palette.BLACK;
    }

    int convertToInverse(int color) {
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);

        if (red > 127 && blue > 127 && green > 127) {
            return Color.argb(255, 0, 0, 0);
        } else {
            return Color.argb(255, 255, 255, 255);
        }
    }
}
