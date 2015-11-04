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

package vogar.target.junit;

import junit.framework.TestCase;

import vogar.target.junit.JUnitRunnerFactory;
import vogar.target.junit3.SimpleTest;
import vogar.target.junit3.SuiteTest;
import vogar.target.junit3.WrongSuiteTest;

/**
 * Tests for {@link JUnitRunnerFactory}
 */
public class JUnitRunnerFactoryTest extends TestCase {
    private JUnitRunnerFactory runnerFactory;

    @Override
    public void setUp() throws Exception {
        runnerFactory = new JUnitRunnerFactory();
    }

    public void test_supports_should_judge_whether_Object_is_not_supported() {
      assertEquals(false, runnerFactory.supports(Object.class));
    }

    public void test_supports_should_judge_whether_SimpleTest_which_inherits_from_TestCase_is_supported() {
        assertEquals(true, runnerFactory.supports(SimpleTest.class));
    }

    public void test_supports_should_judge_whether_WrongSuiteTest_which_has_suite_non_static_method_is_not_supported() {
        assertEquals(false, runnerFactory.supports(WrongSuiteTest.class));
    }

    public void test_supports_should_judge_whether_SuiteTest_which_has_suite_static_method_is_supported() {
        assertEquals(true, runnerFactory.supports(SuiteTest.class));
    }
}
