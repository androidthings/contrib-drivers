/*
 * Copyright 2017 Google Inc.
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

package com.google.android.things.contrib.driver.vcnl4200;

import static com.google.android.things.contrib.driver.testutils.BitsMatcher.hasBitsSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.shortThat;
import static org.mockito.Mockito.times;

import com.google.android.things.contrib.driver.vcnl4200.Vcnl4200.InterruptStatus;
import com.google.android.things.pio.I2cDevice;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class Vcnl4200Test {

    @Mock
    private I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void close() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        vcnl4200.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }

    @Test
    public void getDeviceId() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.getDeviceId();
        Mockito.verify(mI2c).readRegWord(eq(Vcnl4200.REGISTER_DEVICE_ID));
    }

    @Test
    public void getDeviceId_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.getDeviceId();
    }

    @Test
    public void getAlsData() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.getAlsData();
        Mockito.verify(mI2c).readRegWord(eq(Vcnl4200.REGISTER_ALS_DATA));
    }

    @Test
    public void getAlsData_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.getAlsData();
    }

    @Test
    public void getPsData() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.getPsData();
        Mockito.verify(mI2c).readRegWord(eq(Vcnl4200.REGISTER_PROX_DATA));
    }

    @Test
    public void getPsData_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.getPsData();
    }

    @Test
    public void getWhiteData() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.getWhiteData();
        Mockito.verify(mI2c).readRegWord(eq(Vcnl4200.REGISTER_WHITE_DATA));
    }

    @Test
    public void getWhiteData_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.getWhiteData();
    }

    @Test
    public void getInterruptStatus() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.getInterruptStatus();
        Mockito.verify(mI2c).readRegWord(eq(Vcnl4200.REGISTER_INTERRUPT_FLAGS));
    }

    @Test
    public void getInterruptStatus_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.getInterruptStatus();
    }

    @Test
    public void validateInterruptStatus() throws IOException {
        /**
         * Interrupt Status Register, High Byte (details from documentation):
         * 7 | PS_UPFLAG PS code saturation flag
         * 6 | PS_SPFLAG PS enter sunlight protection flag
         * 5 | ALS_IF_L, ALS crossing low THD INT trigger event
         * 4 | ALS_IF_H, ALS crossing high THD INT trigger event
         * 3 | Default = 0, reserved
         * 2 | Default = 0, reserved
         * 1 | PS_IF_CLOSE, PS rise above PS_THDH INT trigger event
         * 0 | PS_IF_AWAY, PS drop below PS_THDL INT trigger event
         */
        InterruptStatus interruptStatus = InterruptStatus.fromStatus((short) (0b10100010 << 8));
        assertTrue(interruptStatus.FLAG_PS_UPFLAG);
        assertFalse(interruptStatus.FLAG_PS_SPFLAG);
        assertTrue(interruptStatus.FLAG_ALS_IF_L);
        assertFalse(interruptStatus.FLAG_ALS_IF_H);
        assertTrue(interruptStatus.FLAG_PS_IF_CLOSE);
        assertFalse(interruptStatus.FLAG_PS_IF_AWAY);
    }

    @Test
    public void setAlsIntegrationTime() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_50MS);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_IT_TIME_50MS)));

        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_100MS);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_IT_TIME_100MS)));

        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_200MS);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_IT_TIME_200MS)));

        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_400MS);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_IT_TIME_400MS)));
    }

    @Test
    public void setAlsIntegrationTime_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_50MS);
    }

    @Test
    public void validateAlsMaxRangeAndSensitivitySet() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);

        Mockito.doReturn((short) Vcnl4200.ALS_IT_TIME_50MS).when(mI2c)
                .readRegWord(Vcnl4200.REGISTER_ALS_CONF);
        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_50MS);
        assertTrue(vcnl4200.getCurrentAlsResolution() == Vcnl4200.ALS_IT_50MS_SENSITIVITY_RANGE[0]);
        assertTrue(vcnl4200.getCurrentAlsMaxRange() == Vcnl4200.ALS_IT_50MS_SENSITIVITY_RANGE[1]);

        Mockito.doReturn((short) Vcnl4200.ALS_IT_TIME_100MS).when(mI2c)
                .readRegWord(Vcnl4200.REGISTER_ALS_CONF);
        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_100MS);
        assertTrue(
                vcnl4200.getCurrentAlsResolution() == Vcnl4200.ALS_IT_100MS_SENSITIVITY_RANGE[0]);
        assertTrue(vcnl4200.getCurrentAlsMaxRange() == Vcnl4200.ALS_IT_100MS_SENSITIVITY_RANGE[1]);

        Mockito.doReturn((short) Vcnl4200.ALS_IT_TIME_200MS).when(mI2c)
                .readRegWord(Vcnl4200.REGISTER_ALS_CONF);
        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_200MS);
        assertTrue(
                vcnl4200.getCurrentAlsResolution() == Vcnl4200.ALS_IT_200MS_SENSITIVITY_RANGE[0]);
        assertTrue(vcnl4200.getCurrentAlsMaxRange() == Vcnl4200.ALS_IT_200MS_SENSITIVITY_RANGE[1]);

        Mockito.doReturn((short) Vcnl4200.ALS_IT_TIME_400MS).when(mI2c)
                .readRegWord(Vcnl4200.REGISTER_ALS_CONF);
        vcnl4200.setAlsIntegrationTime(Vcnl4200.ALS_IT_TIME_400MS);
        assertTrue(
                vcnl4200.getCurrentAlsResolution() == Vcnl4200.ALS_IT_400MS_SENSITIVITY_RANGE[0]);
        assertTrue(vcnl4200.getCurrentAlsMaxRange() == Vcnl4200.ALS_IT_400MS_SENSITIVITY_RANGE[1]);
    }

    @Test
    public void validatePsMaxRangeSet() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);

        Mockito.doReturn((short) Vcnl4200.PS_OUT_RES_12_BITS).when(mI2c)
                .readRegWord(Vcnl4200.REGISTER_PS_CONF_1_2);
        vcnl4200.setPsOutputResolution(Vcnl4200.PS_OUT_RES_12_BITS);
        assertTrue(vcnl4200.getCurrentPsMaxRange() == 0xFFF);

        Mockito.doReturn((short) Vcnl4200.PS_OUT_RES_16_BITS).when(mI2c)
                .readRegWord(Vcnl4200.REGISTER_PS_CONF_1_2);
        vcnl4200.setPsOutputResolution(Vcnl4200.PS_OUT_RES_16_BITS);
        assertTrue(vcnl4200.getCurrentPsMaxRange() == 0xFFFF);
    }

    @Test
    public void setAlsInterruptSwitch() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setAlsInterruptSwitch(Vcnl4200.ALS_INT_SWITCH_ALS_CHANNEL);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_SWITCH_ALS_CHANNEL)));

        vcnl4200.setAlsInterruptSwitch(Vcnl4200.ALS_INT_SWITCH_WHITE_CHANNEL);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_SWITCH_WHITE_CHANNEL)));
    }

    @Test
    public void setAlsInterruptSwitch_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setAlsInterruptSwitch(Vcnl4200.ALS_INT_SWITCH_ALS_CHANNEL);
    }

    @Test
    public void setAlsInterruptPersistence() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setAlsInterruptPersistence(Vcnl4200.ALS_INT_PERSISTENCE_1);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_PERSISTENCE_1)));

        vcnl4200.setAlsInterruptPersistence(Vcnl4200.ALS_INT_PERSISTENCE_2);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_PERSISTENCE_2)));

        vcnl4200.setAlsInterruptPersistence(Vcnl4200.ALS_INT_PERSISTENCE_4);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_PERSISTENCE_4)));

        vcnl4200.setAlsInterruptPersistence(Vcnl4200.ALS_INT_PERSISTENCE_8);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_PERSISTENCE_8)));
    }

    @Test
    public void setAlsInterruptPersistence_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setAlsInterruptPersistence(Vcnl4200.ALS_INT_PERSISTENCE_1);
    }

    @Test
    public void enableAlsInterrupt() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.enableAlsInterrupt(true);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_ENABLE)));

        vcnl4200.enableAlsInterrupt(false);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_INT_DISABLE)));
    }

    @Test
    public void enableAlsInterrupt_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.enableAlsInterrupt(true);
    }

    @Test
    public void enableAlsPower() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.enableAlsPower(true);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_POWER_ON)));

        vcnl4200.enableAlsPower(false);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_CONF),
                shortThat(hasBitsSet((short) Vcnl4200.ALS_POWER_OFF)));
    }

    @Test
    public void enableAlsPower_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.enableAlsPower(true);
    }

    @Test
    public void setAlsInterruptThresholds() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setAlsInterruptThresholds(0.0f, 100.0f);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_LOW_INT_THRESH),
                eq((short) 0x00));
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_HIGH_INT_THRESH),
                eq((short) (100.0f / vcnl4200.getCurrentAlsResolution())));
    }

    @Test
    public void setAlsInterruptThresholds_throwsIfClosedOrOutOfRange() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        mExpectedException.expect(IllegalArgumentException.class);
        vcnl4200.setAlsInterruptThresholds(100.0f, 0.0f);
        mExpectedException.expect(IllegalArgumentException.class);
        vcnl4200.setAlsInterruptThresholds(-1.0f, 0.0f);
        mExpectedException.expect(IllegalArgumentException.class);
        vcnl4200.setAlsInterruptThresholds(0.0f, vcnl4200.getCurrentAlsMaxRange() + 1.0f);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setAlsInterruptThresholds(0.0f, 100.0f);
    }

    @Test
    public void setAlsInterruptThresholds_boundsCheck() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setAlsInterruptThresholds(0.0f, vcnl4200.getCurrentAlsMaxRange());
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_LOW_INT_THRESH),
                eq((short) 0x00));
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_ALS_HIGH_INT_THRESH),
                eq((short) 0xFFFF /* setting maximum threshold */));
    }

    @Test
    public void setPsIredDutyCycle() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsIredDutyCycle(Vcnl4200.PS_IRED_DUTY_CYCLE_1_160);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IRED_DUTY_CYCLE_1_160));

        vcnl4200.setPsIredDutyCycle(Vcnl4200.PS_IRED_DUTY_CYCLE_1_320);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IRED_DUTY_CYCLE_1_320));

        vcnl4200.setPsIredDutyCycle(Vcnl4200.PS_IRED_DUTY_CYCLE_1_640);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IRED_DUTY_CYCLE_1_640));

        vcnl4200.setPsIredDutyCycle(Vcnl4200.PS_IRED_DUTY_CYCLE_1_1280);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IRED_DUTY_CYCLE_1_1280));
    }

    @Test
    public void setPsIredDutyCycle_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsIredDutyCycle(Vcnl4200.PS_IRED_DUTY_CYCLE_1_160);
    }

    @Test
    public void enablePsInterruptPersistence() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsInterruptPersistence(Vcnl4200.PS_INT_PERSISTENCE_1);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_PERSISTENCE_1));

        vcnl4200.setPsInterruptPersistence(Vcnl4200.PS_INT_PERSISTENCE_2);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_PERSISTENCE_2));

        vcnl4200.setPsInterruptPersistence(Vcnl4200.PS_INT_PERSISTENCE_3);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_PERSISTENCE_3));

        vcnl4200.setPsInterruptPersistence(Vcnl4200.PS_INT_PERSISTENCE_4);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_PERSISTENCE_4));
    }

    @Test
    public void enablePsInterruptPersistence_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsInterruptPersistence(Vcnl4200.PS_INT_PERSISTENCE_1);
    }

    @Test
    public void setPsIntegrationTime() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsIntegrationTime(Vcnl4200.PS_IT_1);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IT_1));

        vcnl4200.setPsIntegrationTime(Vcnl4200.PS_IT_1_5);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IT_1_5));

        vcnl4200.setPsIntegrationTime(Vcnl4200.PS_IT_2);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IT_2));

        vcnl4200.setPsIntegrationTime(Vcnl4200.PS_IT_4);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IT_4));

        vcnl4200.setPsIntegrationTime(Vcnl4200.PS_IT_8);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IT_8));

        vcnl4200.setPsIntegrationTime(Vcnl4200.PS_IT_9);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_IT_9));
    }

    @Test
    public void setPsIntegrationTime_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsIntegrationTime(Vcnl4200.PS_IT_1);
    }

    @Test
    public void enablePsPower() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.enablePsPower(true);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_POWER_ON));

        vcnl4200.enablePsPower(false);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_POWER_OFF));
    }

    @Test
    public void enablePsPower_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.enablePsPower(true);
    }

    @Test
    public void enablePsOutputResolution() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsOutputResolution(Vcnl4200.PS_OUT_RES_12_BITS);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_OUT_RES_12_BITS));

        vcnl4200.setPsOutputResolution(Vcnl4200.PS_OUT_RES_16_BITS);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_OUT_RES_16_BITS));
    }

    @Test
    public void enablePsOutputResolution_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsOutputResolution(Vcnl4200.PS_OUT_RES_12_BITS);
    }

    @Test
    public void enablePsInterruptConfiguration() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsInterruptConfiguration(Vcnl4200.PS_INT_CONFIG_DISABLE);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_CONFIG_DISABLE));

        vcnl4200.setPsInterruptConfiguration(Vcnl4200.PS_INT_CONFIG_TRIGGER_BY_AWAY);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_CONFIG_TRIGGER_BY_AWAY));

        vcnl4200.setPsInterruptConfiguration(Vcnl4200.PS_INT_CONFIG_TRIGGER_BY_CLOSING);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_CONFIG_TRIGGER_BY_CLOSING));

        vcnl4200.setPsInterruptConfiguration(Vcnl4200.PS_INT_CONFIG_TRIGGER_BY_CLOSING_AND_AWAY);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_1_2),
                eq((short) Vcnl4200.PS_INT_CONFIG_TRIGGER_BY_CLOSING_AND_AWAY));
    }

    @Test
    public void enablePsInterruptConfiguration_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsInterruptConfiguration(Vcnl4200.PS_INT_CONFIG_DISABLE);
    }

    @Test
    public void setPsMultiPulseNumbers() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsMultiPulseNumbers(Vcnl4200.PS_MULTI_PULSE_1);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_MULTI_PULSE_1));

        vcnl4200.setPsMultiPulseNumbers(Vcnl4200.PS_MULTI_PULSE_2);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_MULTI_PULSE_2));

        vcnl4200.setPsMultiPulseNumbers(Vcnl4200.PS_MULTI_PULSE_4);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_MULTI_PULSE_4));

        vcnl4200.setPsMultiPulseNumbers(Vcnl4200.PS_MULTI_PULSE_8);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_MULTI_PULSE_8));
    }

    @Test
    public void setPsMultiPulseNumbers_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsInterruptConfiguration(Vcnl4200.PS_INT_CONFIG_DISABLE);
    }

    @Test
    public void enablePsSmartPersistence() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.enablePsSmartPersistence(true);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SMART_PERSISTENCE_ENABLE));

        vcnl4200.enablePsSmartPersistence(false);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SMART_PERSISTENCE_DISABLE));
    }

    @Test
    public void enablePsSmartPersistence_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.enablePsSmartPersistence(true);
    }

    @Test
    public void enablePsActiveForceMode() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.enablePsActiveForceMode(true);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_ACTIVE_FORCE_MODE_ENABLE));

        vcnl4200.enablePsActiveForceMode(false);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_ACTIVE_FORCE_MODE_DISABLE));
    }

    @Test
    public void enablePsActiveForceMode_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.enablePsActiveForceMode(true);
    }

    @Test
    public void setPsSunlightImmunity() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsSunlightImmunity(Vcnl4200.PS_SUNLIGHT_IMMUNITY_TYPICAL);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_IMMUNITY_TYPICAL));

        vcnl4200.setPsSunlightImmunity(Vcnl4200.PS_SUNLIGHT_IMMUNITY_2X);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_IMMUNITY_2X));
    }

    @Test
    public void setPsSunlightImmunity_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsSunlightImmunity(Vcnl4200.PS_SUNLIGHT_IMMUNITY_TYPICAL);
    }

    @Test
    public void setPsSunlightProtectMode() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsSunlightProtectMode(Vcnl4200.PS_SUNLIGHT_PROTECT_MODE_00);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_PROTECT_MODE_00));

        vcnl4200.setPsSunlightProtectMode(Vcnl4200.PS_SUNLIGHT_PROTECT_MODE_FF);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_PROTECT_MODE_FF));
    }

    @Test
    public void setPsSunlightProtectMode_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsSunlightProtectMode(Vcnl4200.PS_SUNLIGHT_PROTECT_MODE_00);
    }

    @Test
    public void setPsSunlightCapability() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsSunlightCapability(Vcnl4200.PS_SUNLIGHT_CAP_TYPICAL);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_CAP_TYPICAL));

        vcnl4200.setPsSunlightCapability(Vcnl4200.PS_SUNLIGHT_CAP_1_5);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_CAP_1_5));
    }

    @Test
    public void setPsSunlightCapability_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsSunlightCapability(Vcnl4200.PS_SUNLIGHT_CAP_TYPICAL);
    }

    @Test
    public void setPsOperationMode() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsOperationMode(Vcnl4200.PS_OP_NORMAL_WITH_INT);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_OP_NORMAL_WITH_INT));

        vcnl4200.setPsOperationMode(Vcnl4200.PS_OP_DETECT_LOGIC_OUTPUT);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_OP_DETECT_LOGIC_OUTPUT));
    }

    @Test
    public void setPsOperationMode_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsOperationMode(Vcnl4200.PS_OP_NORMAL_WITH_INT);
    }

    @Test
    public void enablePsCancellationFunction() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.enablePsCancellationFunction(true);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_CANC_ENABLE));

        vcnl4200.enablePsCancellationFunction(false);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_SUNLIGHT_CANC_DISABLE));
    }

    @Test
    public void enablePsCancellationFunction_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.enablePsCancellationFunction(true);
    }

    @Test
    public void setPsInterruptThresholds() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsInterruptThresholds(0, 100);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PROX_LOW_INT_THRESH),
                eq((short) 0));
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PROX_HIGH_INT_THRESH),
                eq((short) 100));
    }

    @Test
    public void setPsInterruptThresholds_throwsIfClosedOrOutOfRange() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        mExpectedException.expect(IllegalArgumentException.class);
        vcnl4200.setPsInterruptThresholds(100, 0);
        mExpectedException.expect(IllegalArgumentException.class);
        vcnl4200.setPsInterruptThresholds(-1, 0);
        mExpectedException.expect(IllegalArgumentException.class);
        vcnl4200.setPsInterruptThresholds(0, vcnl4200.getCurrentPsMaxRange() + 1);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsInterruptThresholds(0x00, 0xFF);
    }

    @Test
    public void setPsInterruptThresholds_boundsCheck() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsInterruptThresholds(0, vcnl4200.getCurrentPsMaxRange());
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PROX_LOW_INT_THRESH),
                eq((short) 0x00));
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PROX_HIGH_INT_THRESH),
                eq((short) (vcnl4200.getCurrentPsMaxRange() & 0xFFFF)));
    }

    @Test
    public void setPsCancellationLevel() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.setPsCancellationLevel((short) 100);
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CANC_LEVEL), eq((short) 100));
    }

    @Test
    public void setPsCancellationLevel_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.setPsCancellationLevel((short) 100);
    }

    @Test
    public void getOnDemandPsData() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        Mockito.reset(mI2c);

        vcnl4200.getOnDemandPsData();
        Mockito.verify(mI2c).writeRegWord(eq(Vcnl4200.REGISTER_PS_CONF_3_MS),
                eq((short) Vcnl4200.PS_TRIGGER_ONE_TIME_CYCLE));
        Mockito.verify(mI2c).readRegWord(Vcnl4200.REGISTER_PROX_DATA);
    }

    @Test
    public void getOnDemandPsData_throwsIfClosed() throws IOException {
        Vcnl4200 vcnl4200 = new Vcnl4200(mI2c);
        vcnl4200.close();
        mExpectedException.expect(IllegalStateException.class);
        vcnl4200.getOnDemandPsData();
    }
}
