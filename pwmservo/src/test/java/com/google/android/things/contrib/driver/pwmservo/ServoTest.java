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
package com.google.android.things.contrib.driver.pwmservo;

import com.google.android.things.pio.Pwm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

public class ServoTest {

    private static final double EPSILON = 1e-15;

    @Mock
    Pwm mPwm;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void setEnabled() throws IOException {
        Servo servo = new Servo(mPwm);
        servo.setEnabled(true);
        Mockito.verify(mPwm).setEnabled(true);
        servo.setEnabled(false);
        Mockito.verify(mPwm).setEnabled(false);
    }

    @Test
    public void setEnabled_throwsIfClosed() throws IOException {
        Servo servo = new Servo(mPwm);
        servo.close();

        mExpectedException.expect(IllegalStateException.class);
        servo.setEnabled(true);
    }

    @Test
    public void setPulseDurationRange() throws IOException {
        Servo servo = new Servo(mPwm);

        final double minPulse = 5, maxPulse = 10;
        servo.setPulseDurationRange(minPulse, maxPulse);
        assertEquals(minPulse, servo.getMinimumPulseDuration(), EPSILON);
        assertEquals(maxPulse, servo.getMaximumPulseDuration(), EPSILON);
    }

    @Test
    public void setPulseDurationRange_throwsIfMinLargerThanMax() throws IOException {
        Servo servo = new Servo(mPwm);
        mExpectedException.expect(IllegalArgumentException.class);
        mExpectedException.expectMessage(equalTo("minMs must be less than maxMs"));
        servo.setPulseDurationRange(10, 5);
    }

    @Test
    public void setPulseDurationRange_throwsIfMinLessThanZero() throws IOException {
        Servo servo = new Servo(mPwm);
        mExpectedException.expect(IllegalArgumentException.class);
        mExpectedException.expectMessage(equalTo("minMs must be greater than 0"));
        servo.setPulseDurationRange(-1, 1);
    }

    @Test
    public void setPulseDurationRange_throwsIfClosed() throws IOException {
        Servo servo = new Servo(mPwm);
        servo.close();

        mExpectedException.expect(IllegalStateException.class);
        servo.setPulseDurationRange(5, 10);
    }

    @Test
    public void setAngleRange() throws IOException {
        Servo servo = new Servo(mPwm);

        final double minAngle = -100, maxAngle = 100;
        servo.setAngleRange(minAngle, maxAngle);
        assertEquals(minAngle, servo.getMinimumAngle(), EPSILON);
        assertEquals(maxAngle, servo.getMaximumAngle(), EPSILON);
    }

    @Test
    public void setAngleRange_throwsIfMinLargerThanMax() throws IOException {
        Servo servo = new Servo(mPwm);
        mExpectedException.expect(IllegalArgumentException.class);
        mExpectedException.expectMessage(equalTo("minAngle must be less than maxAngle"));
        servo.setAngleRange(1, -1);
    }

    @Test
    public void setAngleRange_clampsAngleIfNecessary() throws IOException {
        Servo servo = new Servo(mPwm);
        servo.setAngleRange(-100, 100);
        servo.setAngle(0);

        // clamps too small
        servo.setAngleRange(180, 360);
        assertEquals(180, servo.getAngle(), EPSILON);

        // clamps too large
        servo.setAngleRange(-90, 90);
        assertEquals(90, servo.getAngle(), EPSILON);

        // no clamp
        servo.setAngleRange(0, 180);
        assertEquals(90, servo.getAngle(), EPSILON);
    }


    @Test
    public void setAngle() throws IOException {
        Servo servo = new Servo(mPwm);
        servo.setAngleRange(0, 180);
        servo.setAngle(90);
        assertEquals(90, servo.getAngle(), EPSILON);
    }

    @Test
    public void setAngle_throwsIfTooSmall() throws IOException {
        Servo servo = new Servo(mPwm);

        final double minAngle = 0, maxAngle = 180;
        servo.setAngleRange(minAngle, maxAngle);

        mExpectedException.expect(IllegalArgumentException.class);
        servo.setAngle(-1);
    }

    @Test
    public void setAngle_throwsIfTooLarge() throws IOException {
        Servo servo = new Servo(mPwm);

        final double minAngle = 0, maxAngle = 180;
        servo.setAngleRange(minAngle, maxAngle);

        mExpectedException.expect(IllegalArgumentException.class);
        servo.setAngle(1000);
    }

    @Test
    public void setAngle_throwsIfClosed() throws IOException {
        Servo servo = new Servo(mPwm);
        servo.close();

        mExpectedException.expect(IllegalStateException.class);
        servo.setAngle(90);
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Servo servo = new Servo(mPwm);
        servo.close();
        servo.close(); // should not throw
        Mockito.verify(mPwm, times(1)).close();
    }
}
