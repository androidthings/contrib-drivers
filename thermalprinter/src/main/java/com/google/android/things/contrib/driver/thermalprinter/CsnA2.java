/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.thermalprinter;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * The CsnA2 printer represents a serial printer and contains the direct byte protocols to send and
 * receive from the printer.
 *
 * @see <a href='https://cdn-shop.adafruit.com/datasheets/CSN-A2+User+Manual.pdf'>Datasheet</a>
 */
public class CsnA2 {
    /**
     * The default height in pixels of a printed barcode.
     */
    public static final byte DEFAULT_BARCODE_HEIGHT = 50;
    /**
     * The default spacing between each printed character.
     */
    public static final int DEFAULT_CHAR_SPACING = 6;
    /**
     * The default height of each line.
     */
    public static final byte DEFAULT_LINE_HEIGHT = 30;
    /**
     * The default length of time to heat the dots, 1.2ms.
     */
    public static final int DEFAULT_HEAT_TIME = 120;
    /**
     * The default baud rate for the serial connection to the printer
     */
    public static final int DEFAULT_BAUD_RATE = 19200;

    /**
     * Line feed
     */
    public static final byte CONTROL_ASCII_LF = '\n';
    /**
     * Form feed
     */
    public static final byte CONTROL_ASCII_FF = '\f';
    /**
     * Carriage return
     */
    public static final byte CONTROL_ASCII_CR = '\r';
    /**
     * Device control 2
     */
    public static final byte CONTROL_ASCII_DC2 = 18;
    /**
     * Escape
     */
    static final byte ASCII_ESC = 27;
    /**
     * Group separator
     */
    private static final byte ASCII_GS = 29;

    private static final byte[] COMMAND_EXIT_LOW_POWER_MODE = {ASCII_ESC, '8', 0, 0};
    private static final byte[] COMMAND_CHECK_STATUS = {ASCII_ESC, 'v', 0};
    private static final byte[] COMMAND_PRINT_MODE = {ASCII_ESC, (byte) 33, (byte) 0};
    private static final byte[] COMMAND_UNDERLINE_WEIGHT = {ASCII_ESC, (byte) '-', (byte) 0};
    private static final byte[] COMMAND_INVERSE_ENABLE = {ASCII_GS, (byte) 'B', (byte) 1};
    private static final byte[] COMMAND_INVERSE_DISABLE = {ASCII_GS, (byte) 'B', (byte) 0};
    private static final byte[] COMMAND_PRINTER_ENABLE = {ASCII_ESC, (byte) '=', (byte) 1};
    private static final byte[] COMMAND_PRINTER_DISABLE = {ASCII_ESC, (byte) '=', (byte) 0};
    private static final byte[] COMMAND_FEED_LINES = {ASCII_ESC, (byte) 'd', (byte) 0};
    private static final byte[] COMMAND_FEED_COLS = {ASCII_ESC, (byte) 'J', (byte) 0};
    private static final byte[] COMMAND_FLUSH = {CONTROL_ASCII_FF};
    private static final byte[] COMMAND_JUSTIFY = {ASCII_ESC, (byte) 'a', (byte) 0};
    private static final byte[] COMMAND_PRINT_LABEL_BELOW_BARCODE = {ASCII_GS, 'H', 2};
    private static final byte[] COMMAND_BARCODE_WIDTH_DEFAULT = {ASCII_GS, 'w', 3};
    private static final byte[] COMMAND_BARCODE_TYPE = {ASCII_GS, (byte) 'k', (byte) 0};
    private static final byte[] COMMAND_BARCODE_HEIGHT = {ASCII_GS, (byte) 'h', (byte) 0};
    private static final byte[] COMMAND_RESET = {ASCII_ESC, (byte) '@'};
    private static final byte[] COMMAND_CHAR_SPACING = {ASCII_ESC, (byte) ' ', (byte) 0};
    private static final byte[] COMMAND_CHARSET = {ASCII_ESC, (byte) 'R', (byte) 0};
    private static final byte[] COMMAND_CODEPAGE = {ASCII_ESC, (byte) 't', (byte) 0};
    private static final byte[] COMMAND_LINE_HEIGHT = {ASCII_ESC, (byte) '3', (byte) 0};
    private static final byte[] COMMAND_TEXT_SIZE = {ASCII_GS, (byte) '!', (byte) 0};
    private static final byte[] COMMAND_SLEEP = {ASCII_ESC, (byte) '8', (byte) 0, (byte) 0};
    private static final byte[] COMMAND_DEFAULT_INIT_SEQUENCE =
            {ASCII_ESC, 0x40, ASCII_ESC, 0x37, 0x11, 0x7F, 0x32};


    /* package */ static final byte[] BITMAP_SET_LINE_SPACE_24 = {ASCII_ESC, 0x33, 24};
    /* package */ static final byte[] BITMAP_SELECT_BIT_IMAGE_MODE = {0x1B, 0x2A, 33};

    /**
     * The printer cannot detect paper.
     */
    public static final byte STATUS_PAPER_OUT = 0x04;
    /**
     * The printer voltage is too high, > 9.5v
     */
    public static final byte STATUS_HIGH_VOLTAGE = 0x08;
    /**
     * The printer temperature is too high, > 60°C
     */
    public static final byte STATUS_HIGH_TEMPERATURE = 0x40;

    /**
     * Possible status flags sent from the printer when calling {@link CsnA2#getPrinterStatus()}
     * or using a {@link ThermalPrinter.StatusJob}.
     */
    @IntDef(flag=true, value={STATUS_PAPER_OUT, STATUS_HIGH_VOLTAGE, STATUS_HIGH_TEMPERATURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PrinterStatus {}

    /**
     * Represents possible weights for a text underline
     */
    @IntDef({UNDERLINE_NONE, UNDERLINE_THIN, UNDERLINE_THICK})
    public @interface Underline {}
    public static final byte UNDERLINE_NONE = 0;
    public static final byte UNDERLINE_THIN = 1;
    public static final byte UNDERLINE_THICK = 2;

    /**
     * Represents the justification for printed text
     */
    @IntDef({JUSTIFY_LEFT, JUSTIFY_CENTER, JUSTIFY_RIGHT})
    public @interface Justify {}
    public static final char JUSTIFY_LEFT = 0;
    public static final char JUSTIFY_CENTER = 1;
    public static final char JUSTIFY_RIGHT = 2;

    /**
     * Represents valid barcode encoding types
     */
    @IntDef({UPC_A, UPC_E, EAN13, EAN8, CODE39, ITF, CODABAR, CODE93, CODE128})
    public @interface Barcode{}
    public static final int UPC_A = 65;
    public static final int UPC_E = 66;
    public static final int EAN13 = 67;
    public static final int EAN8 = 68;
    public static final int CODE39 = 69;
    public static final int ITF = 70;
    public static final int CODABAR = 71;
    public static final int CODE93 = 72;
    public static final int CODE128 = 73;

    /**
     * Represents the printed characters used for lower ASCII values 0x23 - 0x7E.
     */
    @SuppressLint("UniqueConstants")
    @IntDef({CHARSET_USA, CHARSET_FRANCE, CHARSET_GERMANY, CHARSET_UK, CHARSET_DENMARK1,
            CHARSET_SWEDEN, CHARSET_ITALY, CHARSET_SPAIN1, CHARSET_JAPAN, CHARSET_NORWAY,
            CHARSET_DENMARK2, CHARSET_SPAIN2, CHARSET_LATINAMERICA, CHARSET_KOREA, CHARSET_SLOVENIA,
            CHARSET_CROATIA, CHARSET_CHINA})
    public @interface Charset{}
    public static final int CHARSET_USA = 0;
    public static final int CHARSET_FRANCE = 1;
    public static final int CHARSET_GERMANY = 2;
    public static final int CHARSET_UK = 3;
    public static final int CHARSET_DENMARK1 = 4;
    public static final int CHARSET_SWEDEN = 5;
    public static final int CHARSET_ITALY = 6;
    public static final int CHARSET_SPAIN1 = 7;
    public static final int CHARSET_JAPAN = 8;
    public static final int CHARSET_NORWAY = 9;
    public static final int CHARSET_DENMARK2 = 10;
    public static final int CHARSET_SPAIN2 = 11;
    public static final int CHARSET_LATINAMERICA = 12;
    public static final int CHARSET_KOREA = 13;
    public static final int CHARSET_SLOVENIA = 14;
    public static final int CHARSET_CROATIA = 14;
    public static final int CHARSET_CHINA = 15;

    /**
     * Represents the printed characters used for upper ASCII values 0x80 - 0xFF.
     */
    @IntDef({CODEPAGE_CP437, CODEPAGE_KATAKANA, CODEPAGE_CP850, CODEPAGE_CP860, CODEPAGE_CP863,
            CODEPAGE_CP865, CODEPAGE_WCP1251, CODEPAGE_CP866, CODEPAGE_MIK, CODEPAGE_CP755,
            CODEPAGE_IRAN, CODEPAGE_CP862, CODEPAGE_WCP1252, CODEPAGE_WCP1253, CODEPAGE_CP852,
            CODEPAGE_CP858, CODEPAGE_IRAN2, CODEPAGE_LATVIAN, CODEPAGE_CP864, CODEPAGE_ISO_8859_1,
            CODEPAGE_CP737, CODEPAGE_WCP1257, CODEPAGE_THAI, CODEPAGE_CP720, CODEPAGE_CP855,
            CODEPAGE_CP857, CODEPAGE_WCP1250, CODEPAGE_CP775, CODEPAGE_WCP1254, CODEPAGE_WCP1255,
            CODEPAGE_WCP1256, CODEPAGE_WCP1258, CODEPAGE_ISO_8859_2, CODEPAGE_ISO_8859_3,
            CODEPAGE_ISO_8859_4, CODEPAGE_ISO_8859_5, CODEPAGE_ISO_8859_6, CODEPAGE_ISO_8859_7,
            CODEPAGE_ISO_8859_8, CODEPAGE_ISO_8859_9, CODEPAGE_ISO_8859_15, CODEPAGE_THAI2,
            CODEPAGE_CP856, CODEPAGE_CP874})
    public @interface Codepage {}
    public static final int CODEPAGE_CP437 = 0;
    public static final int CODEPAGE_KATAKANA = 1;
    public static final int CODEPAGE_CP850 = 2;
    public static final int CODEPAGE_CP860 = 3;
    public static final int CODEPAGE_CP863 = 4;
    public static final int CODEPAGE_CP865 = 5;
    public static final int CODEPAGE_WCP1251 = 6;
    public static final int CODEPAGE_CP866 = 7;
    public static final int CODEPAGE_MIK = 8;
    public static final int CODEPAGE_CP755 = 9;
    public static final int CODEPAGE_IRAN = 10;
    public static final int CODEPAGE_CP862 = 15;
    public static final int CODEPAGE_WCP1252 = 16;
    public static final int CODEPAGE_WCP1253 = 17;
    public static final int CODEPAGE_CP852 = 18;
    public static final int CODEPAGE_CP858 = 19;
    public static final int CODEPAGE_IRAN2 = 20;
    public static final int CODEPAGE_LATVIAN = 21;
    public static final int CODEPAGE_CP864 = 22;
    public static final int CODEPAGE_ISO_8859_1 = 23;
    public static final int CODEPAGE_CP737 = 24;
    public static final int CODEPAGE_WCP1257 = 25;
    public static final int CODEPAGE_THAI = 26;
    public static final int CODEPAGE_CP720 = 27;
    public static final int CODEPAGE_CP855 = 28;
    public static final int CODEPAGE_CP857 = 29;
    public static final int CODEPAGE_WCP1250 = 30;
    public static final int CODEPAGE_CP775 = 31;
    public static final int CODEPAGE_WCP1254 = 32;
    public static final int CODEPAGE_WCP1255 = 33;
    public static final int CODEPAGE_WCP1256 = 34;
    public static final int CODEPAGE_WCP1258 = 35;
    public static final int CODEPAGE_ISO_8859_2 = 36;
    public static final int CODEPAGE_ISO_8859_3 = 37;
    public static final int CODEPAGE_ISO_8859_4 = 38;
    public static final int CODEPAGE_ISO_8859_5 = 39;
    public static final int CODEPAGE_ISO_8859_6 = 40;
    public static final int CODEPAGE_ISO_8859_7 = 41;
    public static final int CODEPAGE_ISO_8859_8 = 42;
    public static final int CODEPAGE_ISO_8859_9 = 43;
    public static final int CODEPAGE_ISO_8859_15 = 44;
    public static final int CODEPAGE_THAI2 = 45;
    public static final int CODEPAGE_CP856 = 46;
    public static final int CODEPAGE_CP874 = 47;

    /**
     * Represents the possible size of text that will be printed.
     */
    @IntDef({SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_XLARGE})
    public @interface TextSize {}
    public static final byte SIZE_SMALL = 10; // Uses a different mechanism
    public static final byte SIZE_MEDIUM = 0;
    public static final byte SIZE_LARGE = 1;
    public static final byte SIZE_XLARGE = 3;

    /**
     * Makes the text very small
     */
    public static final byte TINY_MASK = 1;
    /**
     * Makes the text emphasized
     */
    public static final byte BOLD_MASK = 8;
    /**
     * Makes the text twice as high without being twice as wide
     */
    public static final byte DOUBLE_HEIGHT_MASK = 16;
    /**
     * Makes the text twice as wide without being twice as high
     */
    public static final byte DOUBLE_WIDTH_MASK = 32;

    private UartDevice mUartDevice;

    /**
     * Initializes a new CsnA2.
     *
     * @param uartBus The UART bus to send data through
     */
    /* package */ CsnA2(String uartBus) throws IOException {
        this(uartBus, DEFAULT_BAUD_RATE, null);
    }

    /**
     * Initializes a new CsnA2 with a given baud rate.
     *
     * @param uartBus The UART bus to send data through
     * @param baudRate The symbol rate to send data
     */
    /* package */ CsnA2(String uartBus, int baudRate) throws IOException {
        this(uartBus, baudRate, null);
    }

    /**
     * Initializes a new CsnA2 with a given baud rate and specific heating configurations.
     *
     * @param uartBus The UART bus to send data through
     * @param baudRate The symbol rate to send data
     * @param configuration A configuration object specifying heating parameters for all print tasks
     */
    /* package */ CsnA2(String uartBus, int baudRate, Configuration configuration) throws IOException {
        UartDevice serial = PeripheralManager.getInstance().openUartDevice(uartBus);
        serial.setBaudrate(baudRate);
        serial.setDataSize(8);
        serial.setParity(UartDevice.PARITY_NONE);
        serial.setStopBits(1);
        mUartDevice = serial;

        begin(configuration);
    }

    private void begin(Configuration configuration) throws IOException {
        if (configuration == null) {
            configuration = new Configuration.Builder().build();
        }
        // Customized initialization
        final byte[] commandInitSequence = COMMAND_DEFAULT_INIT_SEQUENCE.clone();
        commandInitSequence[4] = configuration.getHeatingDots(); // Heating dots
        commandInitSequence[5] = configuration.getHeatingTime(); // Heating time
        commandInitSequence[6] = configuration.getHeatingInterval(); // Heating interval

        write(commandInitSequence);
        write(commandEnablePrinter(true)); // Enable printer if we haven't done it yet.
    }

    @VisibleForTesting
    /* package */ CsnA2(UartDevice serial) throws IOException {
        mUartDevice = serial;
        begin(null);
    }

    /**
     * Writes commands directly to the printer.
     *
     * @param data An array of bytes
     */
    /* package */ void write(byte... data) throws IOException {
        mUartDevice.write(data, data.length);
    }

    /**
     * Reads a status byte from the printer. Each bit corresponds to a different field. Masking
     * can be used to isolate a particular part of the status.
     *
     * <table border="1">
     *   <tr>
     *     <td>Bit</td>
     *     <td>Name</td>
     *     <td>Hex Value</td>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>Online</td>
     *     <td>0x01</td>
     *   </tr>
     *   <tr>
     *     <td>2</td>
     *     <td>Paper not detected</td>
     *     <td>0x04</td>
     *   </tr>
     *   <tr>
     *     <td>3</td>
     *     <td>Voltage too high</td>
     *     <td>0x08</td>
     *   </tr>
     *   <tr>
     *     <td>6</td>
     *     <td>Temperature too high</td>
     *     <td>0x40</td>
     *   </tr>
     * </table>
     *
     * @return Byte indicating status of various parts of the printer.
     */
    /* package */ @PrinterStatus int getPrinterStatus() throws IOException {
        // Send cmd, read result.
        // For v264+
        mUartDevice.write(COMMAND_CHECK_STATUS, COMMAND_CHECK_STATUS.length);
        // Block until we receive a data byte back.
        byte[] result = new byte[1];
        int status = 0;
        // Wait to read a UART response or timeout after 3s.
        long timeoutMs = System.currentTimeMillis() + (1000 * 3);
        while (System.currentTimeMillis() < timeoutMs) {
            status = mUartDevice.read(result, 1);
            if ((status & ~0x4d & 0xff) == 0) {
                return result[0];
            } else {
                throw new IOException("Unexpected value returned for printer status: " +
                        Integer.toString(status));
            }
        }
        throw new IOException("Cannot read a response from the printer");
    }

    /**
     * Closes the printer and disables printing.
     */
    /* package */ void close() throws IOException {
        mUartDevice.flush(UartDevice.FLUSH_OUT);
        // setPrinting(false);
        mUartDevice.close();
    }

    /**
     * Returns bytes for doubling height of text without doubling the width.
     *
     * @param enable Whether to double the text height.
     * @return Command to apply that setting.
     */
    public static byte[] commandDoubleHeight(boolean enable) {
        return commandSetPrintMode(DOUBLE_HEIGHT_MASK);
    }

    /**
     * Returns bytes for doubling width of text without doubling the height.
     *
     * @param enable Whether to double the text width.
     * @return Command to apply that setting.
     */
    public static byte[] commandDoubleWidth(boolean enable) {
        return commandSetPrintMode(DOUBLE_WIDTH_MASK);
    }

    /**
     * Returns bytes for applying the text style.
     *
     * @param mode A byte made by masking possible settings {@link #TINY_MASK}, {@link #BOLD_MASK},
     * {@link #DOUBLE_HEIGHT_MASK}, or {@link #DOUBLE_WIDTH_MASK}.
     * @return Command to apply that setting.
     */
    public static byte[] commandSetPrintMode(byte mode) {
        byte[] cmd = COMMAND_PRINT_MODE.clone();
        cmd[2] = DOUBLE_HEIGHT_MASK;
        return cmd;
    }

    /**
     * Returns bytes for inverting text, putting white text on a black background.
     *
     * @param enable Whether to enable text inversion.
     * @return Command to apply that setting.
     */
    public static byte[] commandSetInverse(boolean enable) {
        return enable ? COMMAND_INVERSE_ENABLE : COMMAND_INVERSE_DISABLE;
    }

    /**
     * Returns bytes for temporarily disabling the serial connection to the printer, preventing data
     * from being sent or received.
     *
     * @param enable Whether to enable the printer.
     * @return Command to apply that setting.
     */
    public static byte[] commandEnablePrinter(boolean enable) {
        return enable ? COMMAND_PRINTER_ENABLE : COMMAND_PRINTER_DISABLE;
    }

    /**
     * Returns bytes for underlining printed text.
     *
     * @param weight The weight of the underline, one of {@link #UNDERLINE_NONE},
     * {@link #UNDERLINE_THIN}, or {@link #UNDERLINE_THICK}.
     * @return Command to apply that setting.
     */
    public static byte[] commandUnderlineWeight(@Underline int weight) {
        byte[] cmd = COMMAND_UNDERLINE_WEIGHT.clone();
        cmd[2] = (byte) weight;
        return cmd;
    }

    /**
     * Returns bytes for manually feeding the printer by a number of lines.
     *
     * @param lines The number of lines to feed.
     * @return Command to execute.
     */
    public static byte[] commandFeedLines(byte lines) {
        byte[] cmd = COMMAND_FEED_LINES.clone();
        cmd[2] = lines;
        return cmd;
    }

    /**
     * Returns bytes for manually feeding the printer by a number of columns.
     *
     * @param columns The number of columns to feed.
     * @return Command to execute.
     */
    public static byte[] commandFeedColumns(byte columns) {
        byte[] cmd = COMMAND_FEED_COLS.clone();
        cmd[2] = columns;
        return cmd;
    }

    /**
     * Returns bytes to manually flush the UART buffer.
     *
     * @return Command to execute.
     */
    public static byte[] commandFlush() {
        return COMMAND_FLUSH;
    }

    /**
     * Returns bytes to justify printed text.
     *
     * @param justification The text justification, one of {@link #JUSTIFY_LEFT},
     * {@link #JUSTIFY_CENTER}, {@link #JUSTIFY_RIGHT}.
     * @return Command to apply that setting.
     */
    public static byte[] commandJustify(@Justify int justification) {
        byte[] cmd = COMMAND_JUSTIFY.clone();
        cmd[2] = (byte) justification;
        return cmd;
    }

    /**
     * Returns bytes to print the given text.
     *
     * @param text The text to print.
     * @return Command to execute.
     */
    public static byte[] commandPrintText(@NonNull String text) {
        return text.getBytes();
    }

    /**
     * Returns bytes to print the given barcode.
     *
     * @param text The text encoded in the barcode.
     * @param barcodeType The encoding type for the barcode.
     * @return Command to execute.
     */
    public static byte[] commandPrintBarcode(@NonNull String text, @Barcode int barcodeType) {
        if (text.length() > 255) {
            throw new IllegalArgumentException("Barcode cannot be longer than 255 characters");
        }
        byte[] barcodeTypeCmd = COMMAND_BARCODE_TYPE.clone();
        barcodeTypeCmd[2] = (byte) barcodeType;

        ByteBuffer buffer = ByteBuffer.allocateDirect(512);
        return buffer.put(commandFeedLines((byte) 1)) // Required to before barcode printing
                .put(COMMAND_PRINT_LABEL_BELOW_BARCODE) // Print label below barcode
                .put(COMMAND_BARCODE_WIDTH_DEFAULT) // Barcode width -> 3
                .put(barcodeTypeCmd) // Set the barcode rendering style
                .put((byte) text.length()) // Write text length first
                .put(text.getBytes()) // Write the text
                .put(CONTROL_ASCII_LF) // Mark end of barcode printing
                .array();
    }

    /**
     * Returns bytes to print a bitmap in black-and-white. Any pixel on the bitmap that is not
     * transparent or white will be rendered as black on the printer. The bitmaps should not be
     * larger than 384 pixels.
     *
     * @param bitmap The bitmap to be printed.
     * @return Command to execute.
     */
    public static byte[] commandPrintBitmap(@NonNull Bitmap bitmap) {
        final int BAND_HEIGHT = 24;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * 3 * (height / BAND_HEIGHT) * 10 + 3);
        // Send control bytes in big endian order.
        final byte[] controlByte = {(byte) (0x00ff & width), (byte) ((0xff00 & width) >> 8)};

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        // Bands of pixels are sent that are 8 pixels high.  Iterate through bitmap
        // 24 rows of pixels at a time, capturing bytes representing vertical slices 1 pixel wide.
        // Each bit indicates if the pixel at that position in the slice should be dark or not.
        boolean[] isDark = new boolean[3];
        byte[] bandBytes = new byte[3];
        int[] pixelSlice = new int[3];
        float[] pixelSliceHsv = new float[3];
        for (int row = 0; row < height - 8; row += BAND_HEIGHT) {
            buffer.put(BITMAP_SET_LINE_SPACE_24);
            // Need to send these two sets of bytes at the beginning of each row.
            buffer.put(BITMAP_SELECT_BIT_IMAGE_MODE);
            buffer.put(controlByte);
            // Columns, unlike rows, are one at a time.
            for (int col = 0; col < width; col++) {
                // Reset the values of bandBytes for a new column
                bandBytes[0] = 0;
                bandBytes[1] = 0;
                bandBytes[2] = 0;
                // For each starting row/col position, evaluate each pixel in a column, or "band",
                // 24 pixels high.  Convert into 3 bytes.
                for (int rowOffset = 0; rowOffset < 8; rowOffset++) {
                    // Because the printer only maintains correct height/width ratio
                    // at the highest density, where it takes 24 bit-deep slices, process
                    // a 24-bit-deep slice as 3 bytes.
                    int pixel2Row = row + rowOffset + 8;
                    int pixel3Row = row + rowOffset + 16;
                    // If we go past the bottom of the image, just send white pixels so the printer
                    // doesn't do anything.  Everything still needs to be sent in sets of 3 rows.
                    pixelSlice[0] = bitmap.getPixel(col, row + rowOffset);
                    pixelSlice[1] = (pixel2Row >= bitmap.getHeight()) ?
                            Color.TRANSPARENT : bitmap.getPixel(col, pixel2Row);
                    pixelSlice[2] = (pixel3Row >= bitmap.getHeight()) ?
                            Color.TRANSPARENT : bitmap.getPixel(col, pixel3Row);

                    for (int slice = 0; slice < 3; slice++) {
                        Color.colorToHSV(pixelSlice[slice], pixelSliceHsv);
                        isDark[slice] = pixelSliceHsv[2] < 25; // Hsv[2] -> Value should be 10% dark
                        if (Color.alpha(pixelSlice[slice]) < 25) {
                            isDark[slice] = false;
                        }
                        if (isDark[slice]) {
                            bandBytes[slice] |= 1 << (7 - rowOffset);
                        }
                    }
                }
                // Write row's pixel data
                buffer.put(bandBytes);
            }
            // Finished row
            buffer.put(commandFeedLines((byte) 1));
        }
        // Finish image
        buffer.put(commandFeedLines((byte) 1));
        return buffer.array();
    }

    /**
     * Returns bytes to reset the printer back to its default settings.
     *
     * @return Command to do the reset.
     */
    public static byte[] commandResetPrinter() {
        return COMMAND_RESET;
    }

    /**
     * Returns bytes to set the height of printed barcodes.
     *
     * @param height The height of the barcode in dots.
     * @return Command to change that setting.
     */
    public static byte[] commandBarcodeHeight(byte height) {
        if (height < 1) {
            throw new IllegalArgumentException("Barcode height must be positive");
        }
        byte[] cmd = COMMAND_BARCODE_HEIGHT.clone();
        cmd[2] = height;
        return cmd;
    }

    /**
     * Returns bytes to set the spacing between each character being printed. This is equivalent to
     * the kerning. {@link #DEFAULT_CHAR_SPACING} is the regular height of each line.
     *
     * @param spacing The amount of space between each character.
     * @return Command to change that setting.
     */
    public static byte[] commandCharSpacing(byte spacing) {
        if (spacing < 1) {
            throw new IllegalArgumentException("Character spacing must be positive");
        }
        byte[] cmd = COMMAND_CHAR_SPACING.clone();
        cmd[2] = spacing;
        return cmd;
    }

    /**
     * Returns bytes to set the character set of printed text. This changes characters for lower
     * ASCII values 0x23 - 0x7E. See
     * <a href='https://cdn-shop.adafruit.com/datasheets/CSN-A2+User+Manual.pdf'>the datasheet</a>
     * for more information on each set of symbols.
     *
     * @param charset The character set to be chosen
     * @return Command to change that setting.
     */
    public static byte[] commandCharset(@Charset int charset) {
        byte[] cmd = COMMAND_CHARSET.clone();
        cmd[2] = (byte) charset;
        return cmd;
    }

    /**
     * Returns bytes to set the character set of printed text. This changes characters for upper
     * ASCII values 0x7F - 0xFF. See
     * <a href='https://cdn-shop.adafruit.com/datasheets/CSN-A2+User+Manual.pdf'>the datasheet</a>
     * for more information on each set of symbols.
     *
     * @param codePage The character set to be chosen
     * @return Command to change that setting.
     */
    public static byte[] commandCodepage(@Codepage int codePage) {
        byte[] cmd = COMMAND_CODEPAGE.clone();
        cmd[2] = (byte) codePage;
        return cmd;
    }

    /**
     * Returns bytes to reset all styles for printing text.
     *
     * @return Command to change the setting.
     */
    public static byte[] commandResetTextStyles() {
        return ByteBuffer.allocateDirect(30)
                .put(commandEnablePrinter(true))
                .put(commandJustify(JUSTIFY_LEFT))
                .put(commandSetInverse(false))
                .put(commandLineHeight(DEFAULT_LINE_HEIGHT))
                .put(commandUnderlineWeight(UNDERLINE_NONE))
                .put(commandBarcodeHeight(DEFAULT_BARCODE_HEIGHT))
                .put(commandTextSize(SIZE_SMALL))
                .put(commandCharset(CHARSET_USA))
                .put(commandCodepage(CODEPAGE_CP437))
                .put(commandSetPrintMode((byte) 0))
                .array();
    }

    /**
     * Returns bytes to change the height between lines of text. This is equivalent to the leading.
     * {@link #DEFAULT_LINE_HEIGHT} is the regular height of each line.
     *
     * @param height The height of each line.
     * @return Command to change the setting.
     */
    public static byte[] commandLineHeight(byte height) {
        if (height < 1) {
            throw new IllegalArgumentException("Line height must be positive");
        }
        byte[] cmd = COMMAND_LINE_HEIGHT.clone();
        cmd[2] = height;
        return cmd;
    }

    /**
     * Returns bytes to change size of printed text.
     *
     * @param textSize The relative size of the text. Can be one of {@link #SIZE_SMALL},
     * {@link #SIZE_MEDIUM}, {@link #SIZE_LARGE}, {@link #SIZE_XLARGE}.
     * @return Command to change the setting.
     */
    public static byte[] commandTextSize(@TextSize int textSize) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(6);
        switch (textSize) {
            case SIZE_SMALL:
                // Tiny
                buffer.put(commandSetPrintMode(TINY_MASK));
                break;
            case SIZE_MEDIUM:
                // Small - standard w & h
                buffer.put(commandSetPrintMode((byte) 0));
                break;
            case SIZE_LARGE:
                // Medium - double height
                buffer.put(commandSetPrintMode((byte) 0));
                break;
            case SIZE_XLARGE:
                // Large - double height and width
                buffer.put(commandSetPrintMode((byte) 0));
                break;
        }
        byte[] cmd = COMMAND_TEXT_SIZE.clone();
        cmd[2] = (byte) textSize;
        return cmd;
    }

    /**
     * Returns bytes to set the printer in low-power mode.
     *
     * @param enable Whether to enable the low-power mode.
     * @return Command to change the setting.
     */
    public static byte[] commandLowPowerMode(boolean enable) {
        if (enable) {
            byte[] cmd = COMMAND_SLEEP.clone();
            cmd[2] = 1; // Enter low-power mode after 1 second
            return cmd;
        } else {
            return COMMAND_EXIT_LOW_POWER_MODE;
        }
    }

    /**
     * Represents settings that are configured at the beginning of printer initialization.
     */
    public static class Configuration {
        /**
         * The default number of heating elements, 96 elements.
         */
        public static final int DEFAULT_HEAT_DOTS = 11;

        /**
         * The default length of time to heat the dots, 1.2ms.
         */
        public static final int DEFAULT_HEAT_TIME = 120;

        /**
         * The default length of time between heating actions, 500us.
         */
        public static final int DEFAULT_HEAT_INTERVAL = 50;

        private byte mHeatingDots = DEFAULT_HEAT_DOTS;
        private byte mHeatingTime = DEFAULT_HEAT_TIME;
        private byte mHeatingInterval = DEFAULT_HEAT_INTERVAL;

        /**
         * Gets the maximum number of heating elements that will be active at a time. Printer
         * default is {@link #DEFAULT_HEAT_DOTS}.
         *
         * @return maximum number of heating elements that will be active at a time.
         */
        public byte getHeatingDots() {
            return mHeatingDots;
        }

        /**
         * Gets the length of time that heating elements will be active. Printer default is
         * {@link #DEFAULT_HEAT_TIME}.
         *
         * @return length of time that heating elements will be active.
         */
        public byte getHeatingTime() {
            return mHeatingTime;
        }

        /**
         * Gets the length of delay that heating elements will not be active. Printer default is
         * {@link #DEFAULT_HEAT_INTERVAL}.
         *
         * @return lemngth of delay that heating elements will not be active.
         */
        public byte getHeatingInterval() {
            return mHeatingInterval;
        }

        public static class Builder {
            private Configuration mConfiguration;

            /**
             * Constructs a new Configuration builder.
             */
            public Builder() {
                mConfiguration = new Configuration();
            }

            /**
             * Constructs a new Configuration builder and copies over values from the argument.
             *
             * @param configuration An already existing configuration.
             */
            public Builder(Configuration configuration) {
                mConfiguration = new Configuration();
                mConfiguration.mHeatingDots = configuration.mHeatingDots;
                mConfiguration.mHeatingTime = configuration.mHeatingTime;
                mConfiguration.mHeatingInterval = configuration.mHeatingInterval;
            }

            /**
             * Sets the heating dots, the number of heating elements that will be active at a given
             * time. More heating dots correspond to faster printing but higher peak current.
             *
             * @param heatingDots The number of dots following the formula
             *                    y = 8(x + 1) (dots).
             * @return Returns this Builder for chaining.
             */
            public Builder setHeatingDots(byte heatingDots) {
                mConfiguration.mHeatingDots = heatingDots;
                return this;
            }

            /**
             * Sets the heating time, the duration that a heating element will be active. A longer
             * time corresponds to darker print but a slower speed.
             *
             * @param heatingTime The length of time following the formula
             *                    y = 10x (microseconds)
             * @return Returns this Builder for chaining.
             */
            public Builder setHeatingTime(byte heatingTime) {
                mConfiguration.mHeatingTime = heatingTime;
                return this;
            }

            /**
             * Sets the heating interval, the break between heating elements being active. A longer
             * interval corresponds to sharper print and less peak current but a slower speed.
             *
             * @param heatingInterval The length of time following the formula
             *                        y = 10x (microseconds)
             * @return
             */
            public Builder setHeatingInterval(byte heatingInterval) {
                mConfiguration.mHeatingInterval = heatingInterval;
                return this;
            }

            public Configuration build() {
                return mConfiguration;
            }
        }
    }
}
