/*
 * Copyright (C) 2016 The Android Open Source Project
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

package vogar.target.junit3;

import junit.framework.Test;
import junit.framework.TestResult;

public class SuiteReturnsCustomTest {
    public static Test suite() {
        return new CustomTest();
    }

    static class CustomTest implements Test {
        // This should be marked as @Override but cannot because Vogar's copy of the JUnit classes
        // does not include these methods. They do have to be implemented though because otherwise
        // the tests won't compile as they are compiled against real JUnit.
        public int countTestCases() {
            throw new UnsupportedOperationException();
        }

        // This should be marked as @Override but cannot because Vogar's copy of the JUnit classes
        // does not include these methods. They do have to be implemented though because otherwise
        // the tests won't compile as they are compiled against real JUnit.
        public void run(TestResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return CustomTest.class.getName();
        }
    }
}
