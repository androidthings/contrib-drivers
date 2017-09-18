/*
 * Copyright 2016 Google Inc.
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
package com.google.android.things.contrib.driver.pwmspeaker;

import com.google.android.things.pio.Pwm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.Mockito.times;

public class SpeakerTest {

    @Mock
    Pwm mPwm;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void dutyCycleIsSquareWave() throws IOException {
        Speaker speaker = new Speaker(mPwm);
        Mockito.verify(mPwm).setPwmDutyCycle(50);
    }

    @Test
    public void playStop() throws IOException {
        Speaker speaker = new Speaker(mPwm);

        speaker.play(440);
        Mockito.verify(mPwm).setPwmFrequencyHz(440);
        Mockito.verify(mPwm).setEnabled(true);

        speaker.stop();
        Mockito.verify(mPwm).setEnabled(false);
    }

    @Test
    public void play_throwsIfClosed() throws IOException {
        Speaker speaker = new Speaker(mPwm);
        speaker.close();

        mExpectedException.expect(IllegalStateException.class);
        speaker.play(440);
    }

    @Test
    public void stop_throwsIfClosed() throws IOException {
        Speaker speaker = new Speaker(mPwm);
        speaker.close();

        mExpectedException.expect(IllegalStateException.class);
        speaker.stop();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Speaker speaker = new Speaker(mPwm);
        speaker.close();
        speaker.close(); // should not throw
        Mockito.verify(mPwm, times(1)).close();
    }
}
