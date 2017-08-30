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

package com.google.android.things.contrib.driver.voicehat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VoiceHatInstrumentationTest {
    private static final AudioFormat AUDIO_FORMAT_STEREO =
        new AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(16000)
            .build();

    @Test
    public void testAudioPermissionGranted() {
        String manageAudioDrivers = "com.google.android.things.permission.MANAGE_AUDIO_DRIVERS";
        Context context = InstrumentationRegistry.getContext();
        Assert.assertEquals(PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(manageAudioDrivers));
    }

    @Test
    public void testConstructor() throws IOException {
        assertRaspberryPiOnly();
        VoiceHat voiceHat = new VoiceHat("I2S1", "BCM16", AUDIO_FORMAT_STEREO);
        Assert.assertNotNull(voiceHat.getAudioFormat());
        voiceHat.close();
    }

    @Test
    public void testRegistrationFlow() throws IOException {
        assertRaspberryPiOnly();
        VoiceHat voiceHat = new VoiceHat("I2S1", "BCM16", AUDIO_FORMAT_STEREO);
        voiceHat.registerAudioInputDriver();
        voiceHat.registerAudioOutputDriver();
        voiceHat.unregisterAudioInputDriver();
        voiceHat.unregisterAudioOutputDriver();
        voiceHat.close();
    }

    private void assertRaspberryPiOnly() {
        // This test requires a Raspberry Pi to run
        Assert.assertEquals("rpi3", Build.DEVICE);
    }
}
