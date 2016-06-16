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

package vogar.target.junit;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.runner.JUnitCore;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;
import vogar.monitor.TargetMonitor;
import vogar.target.Profiler;
import vogar.target.SkipPastFilter;
import vogar.target.TargetMonitorRunListener;
import vogar.target.TargetRunner;
import vogar.target.TestEnvironment;

/**
 * Adapts a JUnit3 test for use by vogar.
 */
public final class JUnitTargetRunner implements TargetRunner {

    private final TargetMonitor monitor;
    private final AtomicReference<String> skipPastReference;

    private final TestEnvironment testEnvironment;
    private final int timeoutSeconds;
    private final List<VogarTest> tests;

    public JUnitTargetRunner(TargetMonitor monitor, AtomicReference<String> skipPastReference,
                             TestEnvironment testEnvironment, int timeoutSeconds, List<VogarTest> tests) {
        this.monitor = monitor;
        this.skipPastReference = skipPastReference;
        this.testEnvironment = testEnvironment;
        this.timeoutSeconds = timeoutSeconds;
        this.tests = tests;
    }

    public boolean run(Profiler profiler) {
        // Use JUnit infrastructure to run the tests.
        Runner runner;
        try {
            runner = new VogarTestRunner(tests, testEnvironment, timeoutSeconds, profiler);
        } catch (InitializationError e) {
            throw new IllegalStateException("Could not create VogarTestRunner", e);
        }

        String skipPast = skipPastReference.get();
        if (skipPast != null) {
            try {
                new SkipPastFilter(skipPastReference).apply(runner);
            } catch (NoTestsRemainException ignored) {
                return true;
            }
        }

        try {
            JUnitCore core = new JUnitCore();
            core.addListener(new TargetMonitorRunListener(monitor));
            core.run(runner);
        } catch (VmIsUnstableException e) {
            // If a test reports that the VM is unstable then inform the caller so that the
            // current process can be exited abnormally which will trigger the vogar main process
            // to rerun the tests from after the timing out test.
            return false;
        }

        return true;
    }
}
