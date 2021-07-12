package com.google.android.things.contrib.driver.inkyphat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

class ImageScaler {

    /**
     * Scale in width and height independently, so that sourceBitmap matches dst exactly.
     * This may change the aspect ratio of the sourceBitmap.
     *
     * @param sourceBitmap the bitmap to scale
     * @param maxWidth     the width to scale to
     * @param maxHeight    the height to scale to
     * @return a copy of the source bitmap scaled
     */
    Bitmap fitXY(Bitmap sourceBitmap, int maxWidth, int maxHeight) {
        int bitmapWidth = sourceBitmap.getWidth();
        int bitmapHeight = sourceBitmap.getHeight();
        if (bitmapWidth > maxWidth || bitmapHeight > maxHeight) {
            return scaleBitmap(sourceBitmap, maxWidth, maxHeight);
        } else {
            return sourceBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
    }

    /**
     * Compute a scale that will maintain the original sourceBitmap aspect ratio,
     * but will also ensure that sourceBitmap fits entirely inside the maxWidth & maxHeight.
     * At least one axis (Width or Height) will fit exactly.
     *
     * @param sourceBitmap the bitmap to scale
     * @param maxWidth     the potential max width to scale to
     * @param maxHeight    the potential max height to scale to
     * @return a copy of the source bitmap scaled
     */
    Bitmap fitXorY(Bitmap sourceBitmap, int maxWidth, int maxHeight) {
        int bitmapWidth = sourceBitmap.getWidth();
        int bitmapHeight = sourceBitmap.getHeight();

        int diffWidth = bitmapWidth - maxWidth;
        int diffHeight = bitmapHeight - maxHeight;

        if (diffWidth >= diffHeight) {
            // scale by width
            double widthScaleVector = maxWidth / (double) bitmapWidth;
            int wantedHeight = (int) (bitmapHeight * widthScaleVector);
            return scaleBitmap(sourceBitmap, maxWidth, wantedHeight);
        } else {
            // scale by height
            double heightScaleVector = maxHeight / (double) bitmapHeight;
            int wantedWidth = (int) (bitmapWidth * heightScaleVector);
            return scaleBitmap(sourceBitmap, wantedWidth, maxHeight);
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int wantedWidth, int wantedHeight) {
        Bitmap output = Bitmap.createBitmap(wantedWidth, wantedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix resizeMatrix = new Matrix();
        resizeMatrix.setScale((float) wantedWidth / bitmap.getWidth(), (float) wantedHeight / bitmap.getHeight());
        canvas.drawBitmap(bitmap, resizeMatrix, new Paint());

        return output;
    }

}
