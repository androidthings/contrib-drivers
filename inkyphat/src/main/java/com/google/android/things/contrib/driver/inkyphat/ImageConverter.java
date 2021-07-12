package com.google.android.things.contrib.driver.inkyphat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static com.google.android.things.contrib.driver.inkyphat.InkyPhat.Orientation.PORTRAIT;

class ImageConverter {

    private final InkyPhat.Orientation orientation;
    private final ImageScaler imageScaler;
    private final ColorConverter colorConverter;

    ImageConverter(InkyPhat.Orientation orientation) {
        this.orientation = orientation;
        this.imageScaler = new ImageScaler();
        this.colorConverter = new ColorConverter();
    }

    InkyPhat.PaletteImage convertImage(Bitmap input, InkyPhat.Scale scale) {
        return translateImage(filterImage(input, scale));
    }

    InkyPhat.PaletteImage translateImage(Bitmap input) {
        int width = input.getWidth();
        int height = input.getHeight();
        int[] pixels = new int[width * height];
        input.getPixels(pixels, 0, width, 0, 0, width, height);
        InkyPhat.Palette[] colors = new InkyPhat.Palette[width * height];
        for (int i = 0, pixelsLength = pixels.length; i < pixelsLength; i++) {
            colors[i] = colorConverter.convertARBG888Color(pixels[i]);
        }
        return new InkyPhat.PaletteImage(colors, width);
    }

    Bitmap filterImage(Bitmap sourceBitmap, InkyPhat.Scale scale) {
        return scaleToInkyPhatBounds(sourceBitmap, scale);
    }

    private Bitmap scaleToInkyPhatBounds(Bitmap sourceBitmap, InkyPhat.Scale scale) {
        int bitmapWidth = sourceBitmap.getWidth();
        int bitmapHeight = sourceBitmap.getHeight();
        if (bitmapWidth < getOrientatedWidth() && bitmapHeight < getOrientatedHeight()) {
            return sourceBitmap;
        }

        switch (scale) {
            case FIT_XY:
                return imageScaler.fitXY(sourceBitmap, getOrientatedWidth(), getOrientatedHeight());
            case FIT_X_OR_Y:
                return imageScaler.fitXorY(sourceBitmap, getOrientatedWidth(), getOrientatedHeight());
            default:
                throw new IllegalStateException("Unsupported scale type of " + scale);
        }
    }

    private int getOrientatedWidth() {
        return isIn(PORTRAIT) ? InkyPhat.WIDTH : InkyPhat.HEIGHT;
    }

    private int getOrientatedHeight() {
        return isIn(PORTRAIT) ? InkyPhat.HEIGHT : InkyPhat.WIDTH;
    }

    private boolean isIn(InkyPhat.Orientation orientation) {
        return this.orientation == orientation;
    }

    public InkyPhat.PaletteImage convertText(String text, int color) {
        return convertImage(textAsBitmap(text, 20, color), InkyPhat.Scale.FIT_X_OR_Y);
    }

    private Bitmap textAsBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint(ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(colorConverter.convertToInverse(textColor));
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }
}
