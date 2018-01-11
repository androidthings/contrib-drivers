package com.google.android.things.contrib.driver.inkyphat;

import android.graphics.Bitmap;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

public interface InkyPhat extends AutoCloseable {
    /**
     * Width in pixels, when in the default orientation of {@link InkyPhat.Orientation#PORTRAIT}
     * If the {@link InkyPhat.Orientation} is set to {@link InkyPhat.Orientation#LANDSCAPE} this will be the Height
     */
    int WIDTH = 104;
    /**
     * Height in pixels, when in the default orientation of {@link InkyPhat.Orientation#PORTRAIT}
     * If the {@link InkyPhat.Orientation} is set to {@link InkyPhat.Orientation#LANDSCAPE} this will be the Width
     */
    int HEIGHT = 212;

    /**
     * Set an image to draw with {@link #refresh()} later
     *
     * @param x     the x co-ordinate (or column) to start drawing the images x=0 at
     * @param y     the y co-ordinate (or row) to start drawing the images y=0 at
     * @param image the bitmap to draw (assumed ARGB888)
     * @param scale the scale to use see {@link Scale}
     */
    void setImage(int x, int y, Bitmap image, Scale scale);

    /**
     * Set some text to draw with {@link #refresh()} later
     * the background will be the inverse of the text color
     *
     * @param x     the x co-ordinate (or column) to start writing at
     * @param y     the y co-ordinate (or row) to start writing at
     * @param text  the text to write
     * @param color the color of the written text
     */
    void setText(int x, int y, String text, int color);

    /**
     * Set any pixel in the InkyPhat this is any pixel between {@link #WIDTH} & {@link #HEIGHT}
     * according to the {@link InkyPhat.Orientation} of your display.
     * <p>
     * Colors will be converted in regards to how close they are to
     * {@link InkyPhat.Palette#WHITE}, {@link InkyPhat.Palette#RED}, {@link InkyPhat.Palette#BLACK}
     * <p>
     * You can set the border multiple times it will only update when {@link #refresh()} is called
     * <p>
     * Note, not calling this method for a pixel will leave that pixel as {@link InkyPhat.Palette#WHITE}
     *
     * @param x     the x co-ordinate (or column) to set the pixel on
     * @param y     the y co-ordinate (or row) to set the pixel on
     * @param color the color you want the pixel to be (you can use {@link android.graphics.Color#parseColor(String)} etc
     */
    void setPixel(int x, int y, int color);

    /**
     * Set the border color around the InkyPhat this is the 1x1 pixels around each side
     * You can set the border multiple times it will only update when {@link #refresh()} is called
     *
     * @param color the color you want the border to be
     */
    void setBorder(Palette color);

    /**
     * Draw to the InkyPhat display
     */
    void refresh();

    /**
     * Call close when you have finished with the InkyPhat
     * <p>
     * If you call {@link Factory#create} in an Android lifecycle method
     * remember to call close in the symmetrically matching method
     */
    @Override
    void close();

    /**
     * Use this to create an instance of the {@link InkyPhat}
     */
    class Factory {
        public static InkyPhat create(String spiBus,
                                      String gpioBusyPin, String gpioResetPin, String gpioCommandPin,
                                      Orientation orientation) {
            PeripheralManagerService service = new PeripheralManagerService();
            VersionChecker versionChecker = new VersionChecker(service);
            try {
                VersionChecker.Version version = versionChecker.checkVersion(gpioBusyPin, gpioResetPin);
                SpiDevice device = service.openSpiDevice(spiBus);

                Gpio chipBusyPin = service.openGpio(gpioBusyPin);
                Gpio chipResetPin = service.openGpio(gpioResetPin);
                Gpio chipCommandPin = service.openGpio(gpioCommandPin);

                PixelBuffer pixelBuffer = new PixelBuffer(orientation);
                ImageConverter imageConverter = new ImageConverter(orientation);
                if (version == VersionChecker.Version.ONE) {
                    return new InkyPhatV1(device,
                                          chipBusyPin, chipResetPin, chipCommandPin,
                                          pixelBuffer,
                                          imageConverter,
                                          new ColorConverter()
                    );
                } else if (version == VersionChecker.Version.TWO) {
                    return new InkyPhatV2(device,
                                          chipBusyPin, chipResetPin, chipCommandPin,
                                          pixelBuffer,
                                          imageConverter,
                                          new ColorConverter()
                    );
                } else {
                    throw new IllegalStateException(version + " is not developed or tested yet.");
                }
            } catch (IOException e) {
                throw new IllegalStateException("InkyPhat connection cannot be opened.", e);
            }
        }
    }

    enum Palette {
        BLACK, RED, WHITE
    }

    enum Orientation {
        LANDSCAPE, PORTRAIT
    }

    enum Scale {
        /**
         * Scale in width and height independently, so that sourceBitmap matches dst exactly.
         * This may change the aspect ratio of the sourceBitmap.
         */
        FIT_XY,
        /**
         * Compute a scale that will maintain the original sourceBitmap aspect ratio,
         * but will also ensure that sourceBitmap fits entirely inside the maxWidth & maxHeight.
         * At least one axis (Width or Height) will fit exactly.
         */
        FIT_X_OR_Y
    }

    final class PaletteImage {

        private final Palette[] colors;
        private final int width;

        PaletteImage(Palette[] colors, int width) {
            this.colors = colors;
            this.width = width;
        }

        Palette getPixel(int position) {
            return colors[position];
        }

        int totalPixels() {
            return colors.length;
        }

        int getWidth() {
            return width;
        }
    }
}
