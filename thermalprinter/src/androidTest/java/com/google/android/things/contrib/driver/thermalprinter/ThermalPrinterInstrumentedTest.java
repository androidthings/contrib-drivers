/*
 * Copyright 2017 Google Inc.
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

import static com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter.DELAY_CHARACTER;
import static com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter.DELAY_INIT;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter.BitmapJob;
import com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter.Job;
import com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter.JobStateListener;
import com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter.StatusJob;
import com.google.android.things.contrib.driver.thermalprinter.ThermalPrinter.TextJob;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android Things device. For the sake of this test,
 * the printer is not necessary, as it will cause a lot of extraneous data to be printed.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ThermalPrinterInstrumentedTest {
    private static final String SAMPLE_TEXT = "lorem ipsum";
    private static final Bitmap SAMPLE_BITMAP;
    static {
        // Obtain bitmap
        SAMPLE_BITMAP = BitmapFactory.decodeResource(
                InstrumentationRegistry.getContext().getResources(),
                com.google.android.things.contrib.driver.thermalprinter.test.R.drawable.sample_icon);
    }

    private static final int BYTES_INIT_PRINTER = 10; // 7 bytes to init printer, 3 to enable serial
    private static final int BYTES_CLEANUP_TEXTJOB = 37; // All commands to reset the text styles

    /**
     * Verify that the queue is being scheduled for future writes.
     */
    @Test
    public void testQueueTextBuffering() throws Exception {
        MockThermalPrinter mockThermalPrinter = new MockThermalPrinter();
        Handler handler = new Handler(Looper.getMainLooper());
        long nowMs = System.currentTimeMillis();
        ThermalPrinter printer = new ThermalPrinter(mockThermalPrinter, handler);
        for (int i = 0; i < 5; i++) { // Run 5 times
            printer.enqueue(new TextJob().printText(SAMPLE_TEXT));
            Assert.assertTrue(nowMs <
                printer.mResumeTime + ThermalPrinter.DELAY_CHARACTER * SAMPLE_TEXT.length() * i);
        }

        printer.close();
    }

    /**
     * Verify that the queue is being scheduled for images
     */
    @Test
    public void testQueueImageBuffering() throws Exception {
        MockThermalPrinter mockThermalPrinter = new MockThermalPrinter();
        Handler handler = new Handler(Looper.getMainLooper());
        long nowMs = System.currentTimeMillis();
        ThermalPrinter printer = new ThermalPrinter(mockThermalPrinter, handler);
        long printDuration = printer.enqueue(new BitmapJob().printBitmap(SAMPLE_BITMAP));
        int lineCount = SAMPLE_BITMAP.getHeight() / 24;
        Assert.assertTrue(nowMs < printer.mResumeTime +
                (ThermalPrinter.DELAY_IMAGE_ROW + ThermalPrinter.DELAY_IMAGE_FEEDLINE) * lineCount);
        // Process should take under 10 seconds
        Assert.assertTrue(printDuration < 10 * 1000);

        printer.close();
    }


    /**
     * Verify that the printer initializes correctly and sends the correct data.
     */
    @Test
    public void testPrinterInit() throws Exception {
        MockThermalPrinter mockThermalPrinter = new MockThermalPrinter();
        Handler handler = new Handler(Looper.getMainLooper());
        ThermalPrinter printer = new ThermalPrinter(mockThermalPrinter, handler);

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(DELAY_INIT * 2, TimeUnit.MILLISECONDS);

        Assert.assertEquals(BYTES_INIT_PRINTER, mockThermalPrinter.mBytesSentList.size());
        printer.close();
    }

    /**
     * Verify that the printer initializes correctly and sends the correct data.
     */
    @Test
    public void testTextPrinting() throws Exception {
        MockThermalPrinter mockThermalPrinter = new MockThermalPrinter();
        Handler handler = new Handler(Looper.getMainLooper());
        ThermalPrinter printer = new ThermalPrinter(mockThermalPrinter, handler);
        String stringToPrint = "hello world";
        printer.enqueue(new TextJob().printText(stringToPrint));

        CountDownLatch latch = new CountDownLatch(1);
        long delay = DELAY_INIT + stringToPrint.length() * DELAY_CHARACTER;
        latch.await(delay * 2, TimeUnit.MILLISECONDS);

        Assert.assertEquals(BYTES_INIT_PRINTER + stringToPrint.length() + BYTES_CLEANUP_TEXTJOB,
            mockThermalPrinter.mBytesSentList.size());
        printer.close();
    }

    /**
     * Verify that the printer state listeners work as expected.
     */
    @Test
    public void testPrinterStateListeners() throws IOException, InterruptedException {
        MockThermalPrinter mockThermalPrinter = new MockThermalPrinter();
        Handler handler = new Handler(Looper.getMainLooper());
        ThermalPrinter printer = new ThermalPrinter(mockThermalPrinter, handler);

        final CountDownLatch jobEnqueuedLatch = new CountDownLatch(2);
        final CountDownLatch jobStartedLatch = new CountDownLatch(2);
        final CountDownLatch jobFinishedLatch = new CountDownLatch(2);
        final CountDownLatch queueEmptyLatch = new CountDownLatch(1);

        printer.addJobStateListener(new JobStateListener() {
            @Override
            void onJobEnqueued(Job job) {
                super.onJobEnqueued(job);
                jobEnqueuedLatch.countDown();
            }

            @Override
            void onJobStarted(Job job) {
                super.onJobStarted(job);
                jobStartedLatch.countDown();
            }

            @Override
            void onJobFinished(Job job) {
                super.onJobFinished(job);
                jobFinishedLatch.countDown();
            }

            @Override
            void onQueueEmpty() {
                super.onQueueEmpty();
                queueEmptyLatch.countDown();
            }
        });

        TextJob printSampleText = new TextJob().printText(SAMPLE_TEXT);
        printer.enqueue(printSampleText);
        printer.enqueue(printSampleText);

        CountDownLatch latch = new CountDownLatch(1);
        long delay = DELAY_INIT + SAMPLE_TEXT.length() * DELAY_CHARACTER * 2; // Print text twice
        latch.await(delay * 2, TimeUnit.MILLISECONDS);

        // Verify that all of our latches have been decremented.
        Assert.assertEquals(0, jobEnqueuedLatch.getCount());
        Assert.assertEquals(0, jobStartedLatch.getCount());
        Assert.assertEquals(0, jobFinishedLatch.getCount());
        Assert.assertEquals(0, queueEmptyLatch.getCount());

        printer.close();
    }

    /**
     * Verify that the TextJob only prints the final string.
     */
    @Test
    public void testTextJobPrinting() throws Exception {
        MockThermalPrinter mockThermalPrinter = new MockThermalPrinter();
        Handler handler = new Handler(Looper.getMainLooper());
        ThermalPrinter printer = new ThermalPrinter(mockThermalPrinter, handler);
        String stringToPrint = "hello world";
        String stringToPrint2 = "hello world - 2";
        printer.enqueue(new TextJob().printText(stringToPrint).printText(stringToPrint2));

        CountDownLatch latch = new CountDownLatch(1);
        long delay = DELAY_INIT + stringToPrint2.length() * DELAY_CHARACTER;
        latch.await(delay * 2, TimeUnit.MILLISECONDS);

        Assert.assertEquals(BYTES_INIT_PRINTER + stringToPrint2.length() + BYTES_CLEANUP_TEXTJOB,
                mockThermalPrinter.mBytesSentList.size());
        printer.close();
    }

    /**
     * Verify that we read the status correctly.
     */
    @Test
    public void testPrinterStatus() throws Exception {
        MockThermalPrinter mockThermalPrinter = new MockThermalPrinter();
        Handler handler = new Handler(Looper.getMainLooper());
        final ThermalPrinter printer = new ThermalPrinter(mockThermalPrinter, handler);

        // Listen for status
        printer.addJobStateListener(new JobStateListener() {
            @Override
            void onPrinterStatus(int status) {
                super.onPrinterStatus(status);
                Assert.assertEquals(0x04, status);
                try {
                    printer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        printer.enqueue(new StatusJob());

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(DELAY_INIT * 2, TimeUnit.MILLISECONDS);
    }
}
