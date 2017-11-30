package com.google.android.things.contrib.driver.voicehat;

import android.media.AudioFormat;
import android.os.Build;
import junit.framework.Assert;

/**
 * A collection of common methods across tests.
 */
public class InstrumentationTestUtils {
    /* package */ static final String VOICE_HAT_TRIGGER_GPIO_RPI = "BCM16";
    /* package */ static final String VOICE_HAT_BUTTON_GPIO_RPI = "BCM23";
    /* package */ static final String VOICE_HAT_LED_GPIO_RPI = "BCM25";

    /* package */ static final AudioFormat AUDIO_FORMAT_STEREO = new AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(16000)
            .build();

    /* package */ static void assertRaspberryPiOnly() {
        // This test requires a Raspberry Pi to run
        Assert.assertEquals("rpi3", Build.DEVICE);
    }
}
