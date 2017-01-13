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
package com.google.android.things.contrib.driver.button;

import android.support.test.rule.ActivityTestRule;
import android.view.KeyEvent;

import org.junit.Rule;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ButtonInputDriverInstrumentedTest {

    @Rule
    public ActivityTestRule<ButtonTestActivity> mActivityRule
            = new ActivityTestRule<>(ButtonTestActivity.class);

    @Test
    public void buttonDriverInjectsKeyEvents() throws InterruptedException {
        ButtonTestActivity activity = mActivityRule.getActivity();

        activity.sendMockButtonEvent(true); // press
        KeyEvent event = activity.getNextKeyDownEvent();
        assertEquals(ButtonTestActivity.KEYCODE, event.getKeyCode());

        activity.sendMockButtonEvent(false); // release
        event = activity.getNextKeyUpEvent();
        assertEquals(ButtonTestActivity.KEYCODE, event.getKeyCode());
    }
}
