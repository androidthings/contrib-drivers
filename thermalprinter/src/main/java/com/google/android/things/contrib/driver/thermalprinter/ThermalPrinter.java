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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.Barcode;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.Charset;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.Codepage;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.Configuration;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.Justify;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.PrinterStatus;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.TextSize;
import com.google.android.things.contrib.driver.thermalprinter.CsnA2.Underline;
import com.google.android.things.pio.UartDevice;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A driver for a serial thermal printer. This class allows an Android Things device to print
 * characters, barcodes, and bitmaps on the printer. All print commands are stored in a queue in the
 * order they are sent and scheduled to execute at certain times to avoid overflowing the print
 * buffer. In addition to printing simple text, many text styles can be applied.
 */
public class ThermalPrinter implements AutoCloseable {
    // Delays identified through trial and error
    /* package */ static final long DELAY_INIT = 500;
    /* package */ static final long DELAY_IMAGE_ROW = 1000;
    /* package */ static final long DELAY_IMAGE_FEEDLINE = 10;
    /* package */ static final long DELAY_CHARACTER = 1;
    /* package */ static final long DELAY_COMMAND = 3;

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    // Use this to always have a unique job identifier when one is constructed
    private static int mCurrentJobId = 0;

    private CsnA2 mCsnA2;
    private Handler mHandler;
    /* package */ long mResumeTime;
    /* package */ int mCurrentJobsEnqueued = 0;
    private List<JobStateListener> mEventListeners;

    /**
     * Initializes a new thermal printer.
     *
     * @param uartBus The UART bus to send data through
     */
    public ThermalPrinter(String uartBus) throws IOException {
        this(uartBus, CsnA2.DEFAULT_BAUD_RATE, null, null);
    }

    /**
     * Initializes a new thermal printer.
     *
     * @param uartBus The UART bus to send data through
     * @param configuration A configuration object specifying heating parameters for all print tasks
     */
    public ThermalPrinter(String uartBus, Configuration configuration) throws IOException {
        this(uartBus, CsnA2.DEFAULT_BAUD_RATE, configuration, null);
    }

    /**
     * Initializes a new thermal printer.
     *
     * @param uartBus The UART bus to send data through
     * @param baudRate The symbol rate to send data
     * @param configuration A configuration object specifying heating parameters for all print tasks
     */
    public ThermalPrinter(String uartBus, int baudRate, Configuration configuration)
            throws IOException {
        this(uartBus, baudRate, configuration, null);
    }

    /**
     * Initializes a new thermal printer.
     *
     * @param uartBus The UART bus to send data through
     * @param baudRate The symbol rate to send data
     * @param configuration A configuration object specifying heating parameters for all print tasks
     * @param handler A handler on which commands will be sent. If not supplied the current looper
     *  will be used.
     */
    public ThermalPrinter(String uartBus, int baudRate, Configuration configuration,
            Handler handler) throws IOException {
        mCsnA2 = new CsnA2(uartBus, baudRate, configuration);
        // The first 500ms are meant for initialization. Delay all prints until after.
        mResumeTime = System.currentTimeMillis() + DELAY_INIT;
        if (handler == null) {
            mHandler = new Handler(Looper.myLooper());
        } else {
            mHandler = handler;
        }
    }

    @VisibleForTesting
    /* package */ ThermalPrinter(UartDevice uartDevice, Handler handler) throws IOException {
        mCsnA2 = new CsnA2(uartDevice);
        mHandler = handler;
        // The first 500ms are meant for initialization. Delay all prints until after.
        mResumeTime = System.currentTimeMillis() + DELAY_INIT;
    }

    /**
     * Enqueues a new print job and will execute it as soon as possible.
     *
     * @param job The job to print
     * @return The difference in time it will take to print this job from now in milliseconds.
     */
    public long enqueue(final Job job) {
        // Generate tasks for all settings made to the printer.
        job.build();

        mCurrentJobsEnqueued++;
        final long nowMs = System.currentTimeMillis();
        // Job state - STARTED
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mEventListeners != null) {
                    for (JobStateListener listener : mEventListeners) {
                        listener.onJobStarted(job);
                    }
                }
            }
        }, mResumeTime - nowMs);
        // Handle a StatusJob
        if (job instanceof StatusJob) {
            // Get status and put into listener
            mResumeTime = Math.max(mResumeTime + DELAY_COMMAND, nowMs + DELAY_COMMAND);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        int status = mCsnA2.getPrinterStatus();
                        if (mEventListeners != null) {
                            for (JobStateListener listener : mEventListeners) {
                                listener.onPrinterStatus(status);
                            }
                        }
                    } catch (IOException e) {
                        if (mEventListeners != null) {
                            for (JobStateListener listener : mEventListeners) {
                                listener.onJobError(job, e);
                            }
                        }
                    }
                }
            }, mResumeTime - nowMs);
        }
        // Iterate through each task of the job and enqueue those bytes
        for (final Task task : job.getTasks()) {
            mResumeTime = Math.max(mResumeTime + task.estimatedPrintDuration,
                    nowMs + task.estimatedPrintDuration);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCsnA2.write(task.bytesInJob);
                    } catch (IOException e) {
                        if (mEventListeners != null) {
                            for (JobStateListener listener : mEventListeners) {
                                listener.onJobError(job, e);
                            }
                        }
                    }
                }
            }, mResumeTime - nowMs);
        }

        // Enqueue the main print task if it exists
        if (job.getPrintTask() != null) {
            final Task task = job.getPrintTask();
            mResumeTime = Math.max(mResumeTime + task.estimatedPrintDuration,
                    nowMs + task.estimatedPrintDuration);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCsnA2.write(task.bytesInJob);
                    } catch (IOException e) {
                        if (mEventListeners != null) {
                            for (JobStateListener listener : mEventListeners) {
                                listener.onJobError(job, e);
                            }
                        }
                    }
                }
            }, mResumeTime - nowMs);
        }

        // Job state - ENQUEUED
        if (mEventListeners != null) {
            for (JobStateListener listener : mEventListeners) {
                listener.onJobEnqueued(job);
            }
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Job state - FINISHED
                mCurrentJobsEnqueued--;
                if (mEventListeners != null) {
                    for (JobStateListener listener : mEventListeners) {
                        listener.onJobFinished(job);
                    }
                }
                if (mCurrentJobsEnqueued == 0) {
                    if (mEventListeners != null) {
                        for (JobStateListener listener : mEventListeners) {
                            listener.onQueueEmpty();
                        }
                    }
                }
            }
        }, mResumeTime - nowMs);
        // Return the total duration of the print
        return mResumeTime - nowMs;
    }

    /**
     * Adds a new listener to receive events when state of a job changes.
     * @param listener A listener.
     */
    public void addJobStateListener(JobStateListener listener) {
        if (mEventListeners == null) {
            mEventListeners = new ArrayList<>();
        }
        mEventListeners.add(listener);
    }

    /**
     * Removes a job state listener.
     * @param listener A listener.
     */
    public void removeJobStateListener(JobStateListener listener) {
        if (mEventListeners == null) {
            return;
        }
        mEventListeners.remove(listener);
    }

    /**
     * Closes the connection to the printer and disables connection to the printer.
     */
    @Override
    public void close() throws IOException {
        mCsnA2.write(CsnA2.commandEnablePrinter(false)); // Disable connection to printer.
        mCsnA2.close();
    }

    public abstract static class JobStateListener {
        /**
         * Callback that runs when a job is first enqueued.
         * @param job The job that was enqueued.
         */
        void onJobEnqueued(Job job) {}

        /**
         * Callback that runs when a job is started.
         * @param job The job that was started.
         */
        void onJobStarted(Job job) {}

        /**
         * Callback that runs when a job is completed.
         * @param job The job that was completed.
         */
        void onJobFinished(Job job) {}

        /**
         * Callback that runs when an error happens during a print job.
         *
         * @param job The job that was in the process of running.
         * @param error The exception that was thrown.
         */
        void onJobError(Job job, Exception error) {}

        /**
         * If a {@link StatusJob} is enqueued, the status of the printer will be received using this
         * callback.
         * @param status The printer status. It can be masked with {@link CsnA2#STATUS_PAPER_OUT},
         * {@link CsnA2#STATUS_HIGH_VOLTAGE}, or {@link CsnA2#STATUS_HIGH_TEMPERATURE} to determine
         * that state.
         */
        void onPrinterStatus(@PrinterStatus int status) {}

        /**
         * Callback that runs when there are no jobs currently in the queue.
         */
        void onQueueEmpty() {}
    }

    public static class Job {
        private int mJobId = mCurrentJobId++; // Default printer ID
        private Task mPrintTask;
        private List<Task> mTasks;

        public Job() {
            mTasks = new ArrayList<>();
        }

        /* package */ void addTask(Task task) {
            mTasks.add(task);
        }

        /* package */ List<Task> getTasks() {
            return mTasks;
        }

        /* package */ Task getPrintTask() {
            return mPrintTask;
        }

        /* package */ Job build() {
            return this;
        }

        /**
         * Sets the main task that will be run after all configuration parameters have been set.
         *
         * @param task The task that will actually do the printing. Only one task of this type can
         * be used per job.
         */
        /* package */ void setPrintTask(Task task) {
            mPrintTask = task;
        }

        /**
         * @return Returns the identifier of a job.
         */
        public int getJobId() {
            return mJobId;
        }

        /**
         * Sets an identifier for a given job.
         *
         * @param jobId The identifier for the job.
         * @return Returns itself for chaining.
         */
        public Job setJobId(int jobId) {
            mJobId = jobId;
            return this;
        }
    }

    private static class Task {
        /* package */ final byte[] bytesInJob;
        /* package */ final long estimatedPrintDuration;

        /* package */ Task(byte[] bytesInJob, long estimatedPrintDuration) {
            this.bytesInJob = bytesInJob;
            this.estimatedPrintDuration = estimatedPrintDuration;
        }
    }

    /**
     * A job that prints text.
     */
    public static class TextJob extends Job {
        private boolean mDoubleHeight;
        private boolean mDoubleWidth;
        private boolean mInverse;
        private byte mPrintMode;
        private @Underline int mUnderlineWeight;
        private @Justify int mJustification;
        private @Charset int mCharset;
        private @Codepage int mCodepage;
        private byte mLineHeight;
        private @TextSize int mTextSize;
        private byte mCharSpacing;
        private String mTextToPrint;

        /**
         * Doubles height of text without doubling the width.
         *
         * @param enable Whether to enable double height.
         * @return Returns itself for chaining.
         */
        public TextJob setDoubleHeight(boolean enable) {
            mDoubleHeight = enable;
            return this;
        }

        /**
         * Doubles width of text without doubling the height.
         *
         * @param enable Whether to enable double width.
         * @return Returns itself for chaining.
         */
        public TextJob setDoubleWidth(boolean enable) {
            mDoubleWidth = enable;
            return this;
        }

        /**
         * Sets the text style of the printer
         *
         * @param mode A byte made by masking possible settings {@link CsnA2#TINY_MASK},
         * {@link CsnA2#BOLD_MASK}, {@link CsnA2#DOUBLE_HEIGHT_MASK}, or
         * {@link CsnA2#DOUBLE_WIDTH_MASK}.
         * @return Returns itself for chaining.
         */
        public TextJob setPrintMode(byte mode) {
            mPrintMode = mode;
            return this;
        }

        /**
         * Sets the inversion of the text, putting white text on a black background.
         *
         * @param enable Whether to enable text inversion.
         * @return Returns itself for chaining.
         */
        public TextJob setInverse(boolean enable) {
            mInverse = enable;
            return this;
        }

        /**
         * Sets the underline of text.
         *
         * @param weight The weight of the underline, one of {@link CsnA2#UNDERLINE_NONE},
         * {@link CsnA2#UNDERLINE_THIN}, or {@link CsnA2#UNDERLINE_THICK}.
         * @return Returns itself for chaining.
         */
        public TextJob setUnderlineWeight(@Underline int weight) {
            mUnderlineWeight = weight;
            return this;
        }

        /**
         * Sets the justifcation of text.
         *
         * @param justification The text justification, one of {@link CsnA2#JUSTIFY_LEFT},
         * {@link CsnA2#JUSTIFY_CENTER}, {@link CsnA2#JUSTIFY_RIGHT}.
         * @return Returns itself for chaining.
         */
        public TextJob justify(@Justify int justification) {
            mJustification = justification;
            return this;
        }

        /**
         * Sets the character set of text. This changes characters for lower
         * ASCII values 0x23 - 0x7E. See
         * <a href='https://cdn-shop.adafruit.com/datasheets/CSN-A2+User+Manual.pdf'>the datasheet</a>
         * for more information on each set of symbols.
         *
         * @param charset The character set to be chosen
         * @return Returns itself for chaining.
         */
        public TextJob setCharset(@Charset int charset) {
            mCharset = charset;
            return this;
        }

        /**
         * Sets the character set of text. This changes characters for lower
         * ASCII values 0x7F - 0xFF. See
         * <a href='https://cdn-shop.adafruit.com/datasheets/CSN-A2+User+Manual.pdf'>the datasheet</a>
         * for more information on each set of symbols.
         *
         * @param codepage The character set to be chosen
         * @return Returns itself for chaining.
         */
        public TextJob setCodepage(@Codepage int codepage) {
            mCodepage = codepage;
            return this;
        }

        /**
         * Sets the height between lines of text. This is equivalent to the leading.
         * {@link CsnA2#DEFAULT_LINE_HEIGHT} is the regular height of each line.
         *
         * @param height The height of each line.
         * @return Returns itself for chaining.
         */
        public TextJob setLineHeight(byte height) {
            mLineHeight = height;
            return this;
        }

        /**
         * Changes the size of printed text.
         *
         * @param textSize The relative size of the text. Can be one of {@link CsnA2#SIZE_SMALL},
         * {@link CsnA2#SIZE_MEDIUM}, {@link CsnA2#SIZE_LARGE}, {@link CsnA2#SIZE_XLARGE}.
         * @return Returns itself for chaining.
         */
        public TextJob setTextSize(@TextSize int textSize) {
            mTextSize = textSize;
            return this;
        }

        /**
         * Sets the spacing between each character being printed. This is equivalent to
         * the kerning. {@link CsnA2#DEFAULT_CHAR_SPACING} is the regular height of each line.
         *
         * @param spacing The amount of space between each character.
         * @return Returns itself for chaining.
         */
        public TextJob setCharSpacing(byte spacing) {
            mCharSpacing = spacing;
            return this;
        }

        /**
         * Prints a string of text.
         *
         * @param text The text to print.
         * @return Returns itself for chaining.
         */
        public TextJob printText(String text) {
            mTextToPrint = text;
            return this;
        }

        @Override
        Job build() {
            if (mDoubleHeight) {
                addTask(new Task(CsnA2.commandDoubleHeight(true), DELAY_COMMAND));
            }
            if (mDoubleWidth) {
                addTask(new Task(CsnA2.commandDoubleWidth(true), DELAY_COMMAND));
            }
            if (mPrintMode != 0) {
                addTask(new Task(CsnA2.commandSetPrintMode(mPrintMode), DELAY_COMMAND));
            }
            if (mInverse) {
                addTask(new Task(CsnA2.commandSetInverse(true), DELAY_COMMAND));
            }
            if (mUnderlineWeight != 0) {
                addTask(new Task(CsnA2.commandUnderlineWeight(mUnderlineWeight), DELAY_COMMAND));
            }
            if (mJustification != 0) {
                addTask(new Task(CsnA2.commandJustify(mJustification), DELAY_COMMAND));
            }
            if (mCharset != 0) {
                addTask(new Task(CsnA2.commandCharset(mCharset), DELAY_COMMAND));
            }
            if (mCodepage != 0) {
                addTask(new Task(CsnA2.commandCharset(mCodepage), DELAY_COMMAND));
            }
            if (mLineHeight != 0) {
                addTask(new Task(CsnA2.commandLineHeight(mLineHeight), DELAY_COMMAND));
            }
            if (mTextSize != 0) {
                addTask(new Task(CsnA2.commandTextSize(mTextSize), DELAY_COMMAND));
            }
            if (mCharSpacing != 0) {
                addTask(new Task(CsnA2.commandCharSpacing(mCharSpacing), DELAY_COMMAND));
            }
            setPrintTask(new Task(CsnA2.commandPrintText(mTextToPrint),
                mTextToPrint.length() * DELAY_CHARACTER));
            // Reset text styles
            addTask(new Task(CsnA2.commandResetTextStyles(), DELAY_COMMAND));
            return this;
        }
    }

    public static class BarcodeJob extends Job {
        private byte mBarcodeHeight;
        private @NonNull String mBarcodeLabel;
        private @Barcode int mBarcodeType;

        /**
         * Sets the height of printed barcodes.
         *
         * @param height The height in dots.
         * @return Returns itself for chaining.
         */
        public BarcodeJob setBarcodeHeight(byte height) {
            mBarcodeHeight = height;
            return this;
        }

        /**
         * Prints a barcode.
         *
         * @param barcodeLabel The text encoded in the barcode.
         * @param barcodeType The encoding type for the barcode.
         * @return Returns itself for chaining.
         */
        public BarcodeJob printBarcode(@NonNull String barcodeLabel, @Barcode int barcodeType) {
            mBarcodeLabel = barcodeLabel;
            mBarcodeType = barcodeType;
            return this;
        }

        @Override
        Job build() {
            if (mBarcodeHeight != 0) {
                addTask(new Task(CsnA2.commandBarcodeHeight(mBarcodeHeight), DELAY_COMMAND));
            }
            setPrintTask(new Task(CsnA2.commandPrintBarcode(mBarcodeLabel, mBarcodeType),
                mBarcodeLabel.length() * DELAY_CHARACTER));
            return this;
        }
    }

    public static class BitmapJob extends Job {
        private Bitmap mBitmap;

        /**
         * Prints a bitmap in black-and-white. Any pixel on the bitmap that is not transparent or
         * white will be rendered as black on the printer. The bitmaps cannot be larger than 384
         * pixels.
         *
         * @param bitmap The bitmap to be printed.
         * @return Returns itself for chaining.
         */
        public BitmapJob printBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            return this;
        }

        @Override
        Job build() {
            // Check bitmap width
            if (mBitmap.getWidth() > 384) {
                throw new IllegalArgumentException("Bitmap cannot be larger than 384 pixels wide");
            }
            // Due to the large amount of data that a bitmap may contain, this must be broken up
            // into smaller tasks.
            // Copied the `CsnA2.commandPrintBitmap` method, but modified to break into several
            // tasks instead of a single ByteBuffer.
            final int BAND_HEIGHT = 24;

            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();

            // Send control bytes in big endian order.
            final byte[] controlByte = {(byte) (0x00ff & width), (byte) ((0xff00 & width) >> 8)};

            int[] pixels = new int[width * height];
            mBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            // Bands of pixels are sent that are 8 pixels high.  Iterate through bitmap
            // 24 rows of pixels at a time, capturing bytes representing vertical slices 1 pixel wide.
            // Each bit indicates if the pixel at that position in the slice should be dark or not.
            boolean[] isDark = new boolean[3];
            byte[] bandBytes = new byte[3];
            int[] pixelSlice = new int[3];
            float[] pixelSliceHsv = new float[3];
            for (int row = 0; row < height - 8; row += BAND_HEIGHT) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(8 + width * 3);
                buffer.put(CsnA2.BITMAP_SET_LINE_SPACE_24);
                // Need to send these two sets of bytes at the beginning of each row.
                buffer.put(CsnA2.BITMAP_SELECT_BIT_IMAGE_MODE);
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
                        pixelSlice[0] = mBitmap.getPixel(col, row + rowOffset);
                        pixelSlice[1] = (pixel2Row >= mBitmap.getHeight()) ?
                                Color.TRANSPARENT : mBitmap.getPixel(col, pixel2Row);
                        pixelSlice[2] = (pixel3Row >= mBitmap.getHeight()) ?
                                Color.TRANSPARENT : mBitmap.getPixel(col, pixel3Row);

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
                }
                // Write row's pixel data
                addTask(new Task(buffer.array(), DELAY_IMAGE_ROW));
                // Finished row
                addTask(new Task(CsnA2.commandFeedLines((byte) 1), DELAY_IMAGE_FEEDLINE));
            }
            // Finish image
            addTask(new Task(CsnA2.commandFeedLines((byte) 1), DELAY_IMAGE_FEEDLINE));
            return this;
        }
    }

    /**
     * A class representing individual tasks that can be executed on a printer. Only one method
     * can be called for each job, so they are represented as static methods with a private
     * constructor.
     */
    private static class PrinterJob extends Job {
        /**
         * Temporarily enables or disables the serial connection to the printer, preventing data
         * from being sent or received.
         *
         * @param enable Whether to enable the printer.
         * @return Returns itself for chaining.
         */
        public static PrinterJob enablePrinter(boolean enable) {
            PrinterJob printerJob = new PrinterJob();
            printerJob.setPrintTask(new Task(CsnA2.commandEnablePrinter(enable), DELAY_COMMAND));
            return printerJob;
        }

        /**
         * Sets the printer in low-power mode.
         *
         * @param enable Whether to enable the low-power mode.
         * @return Returns itself for chaining.
         */
        public static PrinterJob enableLowPowerMode(boolean enable) {
            PrinterJob printerJob = new PrinterJob();
            printerJob.setPrintTask(new Task(CsnA2.commandLowPowerMode(enable), DELAY_COMMAND));
            return printerJob;
        }

        /**
         * Resets the printer back to its default settings.
         *
         * @return Returns itself for chaining.
         */
        public static PrinterJob resetPrinter() {
            PrinterJob printerJob = new PrinterJob();
            printerJob.setPrintTask(new Task(CsnA2.commandResetPrinter(), DELAY_COMMAND));
            return printerJob;
        }

        /**
         * Writes the data of a given task to the printer.
         *
         * @param task A byte-array containing the command to run.
         * @return Returns itself for chaining.
         */
        public static PrinterJob runTask(byte[] task) {
            PrinterJob printerJob = new PrinterJob();
            printerJob.setPrintTask(new Task(task, DELAY_CHARACTER * task.length));
            return printerJob;
        }


        /**
         * Manually feeds the printer by a given number of lines.
         *
         * @param lines The number of lines to feed.
         * @return Returns itself for chaining.
         */
        public static PrinterJob feedLines(byte lines) {
            PrinterJob printerJob = new PrinterJob();
            printerJob.setPrintTask(new Task(CsnA2.commandFeedLines(lines), DELAY_COMMAND));
            return printerJob;
        }

        /**
         * Manually feeds the printer by a given number of columns.
         *
         * @param columns The number of lines to columns.
         * @return Returns itself for chaining.
         */
        public static PrinterJob feedColumns(byte columns) {
            PrinterJob printerJob = new PrinterJob();
            printerJob.setPrintTask(new Task(CsnA2.commandFeedColumns(columns), DELAY_COMMAND));
            return printerJob;
        }
    }

    /**
     * A job that will return with the current status of the printer in
     * {@link JobStateListener#onPrinterStatus(int)}. Each bit corresponds to a different field.
     * Masking can be used to isolate a particular part of the status.
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
     */
    public static class StatusJob extends Job {} // Handle in the queue
}
