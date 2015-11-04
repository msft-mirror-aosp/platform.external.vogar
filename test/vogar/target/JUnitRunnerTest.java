/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import vogar.Result;
import vogar.monitor.TargetMonitor;
import vogar.target.junit.JUnitRunner;
import vogar.target.junit3.FailTest;
import vogar.target.junit3.LongTest;
import vogar.target.junit3.LongTest2;
import vogar.target.junit3.SimpleTest;
import vogar.target.junit3.SimpleTest2;
import vogar.target.junit3.SuiteTest;
import vogar.target.junit3.WrongSuiteTest;

// This test class is designed for both JUnit3 and newer than JUnit4.8.2
// because it can work for original vogar which uses JUnit3 and new vogar which uses JUnit4.8.2
// If you test it with JUnit4, please test with newer than JUnit4.8.2 in console because Eclipse Helios uses JUnit4.8.1

// for JUnit 3: java -cp bin:lib/junit-3.8.2.jar:lib/mockito-all-1.8.5.jar junit.textui.TestRunner vogar.target.JUnitRunnerTest
// for JUnit 4: java -cp bin:lib/junit-4.8.2.jar:lib/mockito-all-1.8.5.jar org.junit.runner.JUnitCore vogar.target.JUnitRunnerTest vogar.target.JUnit4RunnerTest
public class JUnitRunnerTest extends TestCase {
    private Runner runner;
    private TargetMonitor monitor;
    private TestEnvironment testEnvironment = new TestEnvironment();
    private final AtomicReference<String> skipPastReference = new AtomicReference<String>();

    public void setUp() {
        monitor = mock(TargetMonitor.class);
    }

    public void test_run_for_SimpleTest_should_perform_test() {
        Class<?> target = SimpleTest.class;
        Runner runner =
            new JUnitRunner(monitor, "", null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple", "");
        verify(monitor).outcomeFinished(Result.SUCCESS);
    }

    public void test_run_for_SuiteTest_should_perform_tests() {
        Class<?> target = SuiteTest.class;
        Runner runner =
            new JUnitRunner(monitor, "", null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor).outcomeStarted(runner,
                "vogar.target.junit3.SimpleTest#testSimple", "");
        verify(monitor).outcomeStarted(runner,
                "vogar.target.junit3.SimpleTest2#testSimple1", "");
        verify(monitor).outcomeStarted(runner,
                "vogar.target.junit3.SimpleTest2#testSimple2", "");
        verify(monitor).outcomeStarted(runner,
                "vogar.target.junit3.SimpleTest2#testSimple3", "");
        verify(monitor, times(4)).outcomeFinished(Result.SUCCESS);
    }

    public void test_run_for_SimpleTest2_with_ActionName_should_perform_test() {
        Class<?> target = SimpleTest2.class;
        String actionName = "actionName";
        Runner runner = new JUnitRunner(
            monitor, actionName, null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple1", actionName);
        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple2", actionName);
        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple3", actionName);
        verify(monitor, times(3)).outcomeFinished(Result.SUCCESS);
    }

    public void test_run_for_SimpleTest2_limiting_to_1method_should_perform_test() {
        Class<?> target = SimpleTest2.class;
        String actionName = "actionName";
        Runner runner = new JUnitRunner(
            monitor, actionName, null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, new String[] { "testSimple2" });

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple2", actionName);
        verify(monitor).outcomeFinished(Result.SUCCESS);
    }

    public void test_run_for_SimpleTest2_limiting_to_2methods_should_perform_test() {
        Class<?> target = SimpleTest2.class;
        String actionName = "actionName";
        Runner runner = new JUnitRunner(
            monitor, actionName, null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, new String[] { "testSimple2", "testSimple3" });

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple2", actionName);
        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple3", actionName);
        verify(monitor, times(2)).outcomeFinished(Result.SUCCESS);
    }

    public void test_limiting_to_1method_and_run_for_SimpleTest2_should_perform_test() {
        Class<?> target = SimpleTest2.class;
        String actionName = "actionName";
        Runner runner = new JUnitRunner(
            monitor, actionName, "testSimple2", target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple2", actionName);
        verify(monitor).outcomeFinished(Result.SUCCESS);
    }

    // JUnit3 can't perform test by indicating test method in test suite
    public void test_limiting_to_1method_and_run_for_SuiteTest_should_throw_exception() {
        Class<?> target = SuiteTest.class;

        try {
            Runner runner = new JUnitRunner(
                monitor, "", "testSimple", target, skipPastReference, testEnvironment, 0);
            runner.run("", null, null);
            fail("should throw ClassCastException.");
        } catch (ClassCastException e) {
        }
    }

    public void test_limiting_to_wrong_1method_and_run_for_SimpleTest2_should_fail_test() {
        Class<?> target = SimpleTest2.class;
        String actionName = "actionName";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        Runner runner = new JUnitRunner(
            monitor, actionName, "testSimple5", target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple5", actionName);
        verify(monitor).outcomeFinished(Result.EXEC_FAILED);

        String outStr = baos.toString();
        assertTrue(outStr
                .contains("junit.framework.AssertionFailedError: Method " + '"'
                        + "testSimple5" + '"' + " not found"));
    }

    public void test_run_for_SimpleTest2_limiting_to_1method_with_both_run_should_perform_test() {
        Class<?> target = SimpleTest2.class;
        String actionName = "actionName";
        Runner runner = new JUnitRunner(
            monitor, actionName, "testSimple3", target, skipPastReference, testEnvironment, 0);
        runner.run("", null, new String[] { "testSimple2" });

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSimple2", actionName);
        verify(monitor).outcomeFinished(Result.SUCCESS);
    }

    public void test_run_for_FailTest_should_perform_test() {
        Class<?> target = FailTest.class;
        String actionName = "actionName";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        Runner runner = new JUnitRunner(
            monitor, actionName, null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testSuccess", actionName);
        verify(monitor).outcomeStarted(runner, target.getName() + "#testFail",
                actionName);
        verify(monitor).outcomeStarted(runner,
                target.getName() + "#testThrowException", actionName);
        verify(monitor).outcomeFinished(Result.SUCCESS);
        verify(monitor, times(2)).outcomeFinished(Result.EXEC_FAILED);

        String outStr = baos.toString();
        assertTrue(outStr
                .contains("junit.framework.AssertionFailedError: failed."));
        assertTrue(outStr.contains("java.lang.RuntimeException: exceptrion"));
    }

    public void test_run_for_LongTest_with_time_limit_should_report_time_out() {
        Class<?> target = LongTest.class;
        String actionName = "actionName";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        Runner runner = new JUnitRunner(
            monitor, actionName, null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor).outcomeStarted(runner, target.getName() + "#test",
                actionName);
        verify(monitor).outcomeFinished(Result.EXEC_FAILED);

        String outStr = baos.toString();
        assertTrue(outStr.contains("java.util.concurrent.TimeoutException"));
    }

    public void test_run_for_LongTest2_with_time_limit_should_not_report_time_out() {
        Class<?> target = LongTest2.class;
        String actionName = "actionName";

        Runner runner = new JUnitRunner(
            monitor, actionName, null, target, skipPastReference, testEnvironment, 0);
        runner.run("", null, null);

        verify(monitor, times(8)).outcomeFinished(Result.SUCCESS);
    }
}
