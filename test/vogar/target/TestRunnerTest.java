/*
 * Copyright (C) 2015 The Android Open Source Project
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

package vogar.target;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link TestRunner}
 */
@RunWith(JUnit4.class)
public class TestRunnerTest {

    @Test
    public void testConstructor_WithSkipPast() {
        TestRunner runner = new TestRunner(new ArrayList<>(Arrays.asList("--skipPast", "fred")));
        String skipPast = runner.skipPastReference.get();
        assertEquals("fred", skipPast);
    }
}