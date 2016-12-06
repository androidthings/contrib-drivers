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

package com.google.android.things.contrib.driver.apa102;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class BytesMatcherTest {
    @Test
    public void match() throws IOException {
        BytesMatcher bm = new BytesMatcher("foo".getBytes());
        assertFalse(bm.matches("fo".getBytes()));
        assertTrue(bm.matches("foo".getBytes()));
        assertTrue(bm.matches("hopfoobar".getBytes()));
        assertFalse(bm.matches("noneofthose".getBytes()));
    }

    @Test
    public void matchEmpty() throws IOException {
        BytesMatcher bm = new BytesMatcher("".getBytes());
        assertTrue(bm.matches("foo".getBytes()));
    }

}
