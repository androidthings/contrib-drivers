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

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VoiceHatInstrumentationTest {
    private VoiceHat mVoiceHat;

    /**
     * Verify that the permission is granted to manage audio drivers.
     */
    @Test
    public void testAudioPermissionGranted() {
        String manageAudioDrivers = "com.google.android.things.permission.MANAGE_AUDIO_DRIVERS";
        Context context = InstrumentationRegistry.getContext();
        Assert.assertEquals(PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(manageAudioDrivers));
    }

    /**
     * Verify that a VoiceHat can be created and the audio format is non-null.
     */
    @Test
    public void testConstructor() throws IOException {
        InstrumentationTestUtils.assertRaspberryPiOnly();
        mVoiceHat = new VoiceHat(InstrumentationTestUtils.VOICE_HAT_I2S_RPI,
            InstrumentationTestUtils.VOICE_HAT_TRIGGER_GPIO_RPI,
            InstrumentationTestUtils.AUDIO_FORMAT_STEREO);
        Assert.assertNotNull(mVoiceHat.getAudioFormat());
        Assert.assertNotNull(mVoiceHat.getDac());
    }

    /**
     * Verify that a VoiceHat can be created, registered, and unregistered without issue.
     */
    @Test
    public void testRegistrationFlow() throws IOException {
        InstrumentationTestUtils.assertRaspberryPiOnly();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    mVoiceHat = new VoiceHat(InstrumentationTestUtils.VOICE_HAT_I2S_RPI,
                        InstrumentationTestUtils.VOICE_HAT_TRIGGER_GPIO_RPI,
                        InstrumentationTestUtils.AUDIO_FORMAT_STEREO);
                    mVoiceHat.registerAudioInputDriver();
                    mVoiceHat.registerAudioOutputDriver();
                    mVoiceHat.unregisterAudioInputDriver();
                    mVoiceHat.unregisterAudioOutputDriver();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Verify that a VoiceHat doesn't fail if driver methods are called several times.
     */
    @Test
    public void testEverythingTwice() throws IOException {
        InstrumentationTestUtils.assertRaspberryPiOnly();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    mVoiceHat = new VoiceHat(InstrumentationTestUtils.VOICE_HAT_I2S_RPI,
                        InstrumentationTestUtils.VOICE_HAT_TRIGGER_GPIO_RPI,
                        InstrumentationTestUtils.AUDIO_FORMAT_STEREO);
                    mVoiceHat.registerAudioInputDriver();
                    mVoiceHat.registerAudioInputDriver();

                    mVoiceHat.registerAudioOutputDriver();
                    mVoiceHat.registerAudioOutputDriver();

                    mVoiceHat.unregisterAudioInputDriver();
                    mVoiceHat.unregisterAudioInputDriver();

                    mVoiceHat.unregisterAudioOutputDriver();
                    mVoiceHat.unregisterAudioOutputDriver();

                    mVoiceHat.close();
                    mVoiceHat.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    /**
     * Verify that a VoiceHat can 'unregister' a driver that was never registered.
     */
    @Test
    public void testUnregisterUndefinedDriver() throws IOException {
        InstrumentationTestUtils.assertRaspberryPiOnly();
        mVoiceHat = new VoiceHat(InstrumentationTestUtils.VOICE_HAT_I2S_RPI,
            InstrumentationTestUtils.VOICE_HAT_TRIGGER_GPIO_RPI,
            InstrumentationTestUtils.AUDIO_FORMAT_STEREO);
        mVoiceHat.unregisterAudioInputDriver();
        mVoiceHat.unregisterAudioOutputDriver();
    }

    @After
    public void closeVoiceHat() throws IOException {
        if (mVoiceHat != null) {
            mVoiceHat.close();
        }
    }
}
