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
package com.google.android.things.contrib.driver.cap12xx;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

public class Cap12xxInputDriverInstrumentedTest {

    @Rule
    public ActivityTestRule<Cap12xxTestActivity> mActivityRule
            = new ActivityTestRule<>(Cap12xxTestActivity.class);

    @Test
    public void buttonDriverInjectsKeyEvents() throws IOException {
        Cap12xxTestActivity activity = mActivityRule.getActivity();
        activity.testKeyEvents();
    }
}
