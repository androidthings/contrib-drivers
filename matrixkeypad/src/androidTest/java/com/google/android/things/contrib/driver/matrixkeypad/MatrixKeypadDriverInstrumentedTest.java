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
package com.google.android.things.contrib.driver.matrixkeypad;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import android.content.pm.PackageManager;
import android.support.test.rule.ActivityTestRule;
import android.view.KeyEvent;
import org.junit.Rule;
import org.junit.Test;

public class MatrixKeypadDriverInstrumentedTest {

    @Rule
    public ActivityTestRule<MatrixKeypadActivity> mActivityRule
            = new ActivityTestRule<>(MatrixKeypadActivity.class);

    @Test
    public void checkPermissionsGranted() {
        String MANAGE_INPUT_DRIVERS = "com.google.android.things.permission.MANAGE_INPUT_DRIVERS";
        MatrixKeypadActivity activity = mActivityRule.getActivity();

        assertEquals(PackageManager.PERMISSION_GRANTED,
                activity.checkSelfPermission(MANAGE_INPUT_DRIVERS));
    }

    @Test
    public void keypadDriverInjectsKeyEvents() throws InterruptedException {
        MatrixKeypadActivity activity = mActivityRule.getActivity();

        activity.sendMockKeyEvent(MatrixKeypadActivity.mKeyCodes[0], true); // press
        KeyEvent event = activity.getNextKeyDownEvent();
        assertNotNull(event);
        assertEquals(MatrixKeypadActivity.mKeyCodes[0], event.getKeyCode());

        activity.sendMockKeyEvent(MatrixKeypadActivity.mKeyCodes[0], false); // release
        event = activity.getNextKeyUpEvent();
        assertNotNull(event);
        assertEquals(MatrixKeypadActivity.mKeyCodes[0], event.getKeyCode());
    }
}
