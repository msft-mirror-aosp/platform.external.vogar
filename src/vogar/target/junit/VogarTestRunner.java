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

package vogar.target.junit;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import vogar.target.Profiler;
import vogar.target.TestEnvironment;
import vogar.util.Threads;

/**
 * A {@link org.junit.runner.Runner} that can run a list of {@link VogarTest} instances.
 */
public class VogarTestRunner extends ParentRunner<VogarTest> {

    private final List<VogarTest> children;

    private final TestEnvironment testEnvironment;
    private final int timeoutSeconds;

    private final ExecutorService executor = Executors.newCachedThreadPool(
            Threads.daemonThreadFactory("testrunner"));
    private final Profiler profiler;

    private boolean vmIsUnstable;

    public VogarTestRunner(List<VogarTest> children, TestEnvironment testEnvironment,
                           int timeoutSeconds, Profiler profiler)
            throws InitializationError {
        super(VogarTestRunner.class);
        this.children = children;
        this.testEnvironment = testEnvironment;
        this.timeoutSeconds = timeoutSeconds;
        this.profiler = profiler;
    }

    @Override
    protected List<VogarTest> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(VogarTest child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(final VogarTest child, RunNotifier notifier) {
        runLeaf(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                runWithTimeout(child);
            }
        }, describeChild(child), notifier);

        // Abort the test run if the VM is deemed unstable, i.e. the previous test timed out.
        // Throw this after the results of the previous test have been reported.
        if (vmIsUnstable) {
            throw new VmIsUnstableException();
        }
    }

    /**
     * Runs the test on another thread. If the test completes before the
     * timeout, this reports the result normally. But if the test times out,
     * this reports the timeout stack trace and begins the process of killing
     * this no-longer-trustworthy process.
     */
    private void runWithTimeout(final VogarTest test) throws Throwable {
        testEnvironment.reset();

        // Start the test on a background thread.
        final AtomicReference<Thread> executingThreadReference = new AtomicReference<>();
        Future<Throwable> result = executor.submit(new Callable<Throwable>() {
            public Throwable call() throws Exception {
                executingThreadReference.set(Thread.currentThread());
                try {
                    if (profiler != null) {
                        profiler.start();
                    }
                    test.run();
                    return null;
                } catch (Throwable throwable) {
                    return throwable;
                } finally {
                    if (profiler != null) {
                        profiler.stop();
                    }
                }
            }
        });

        // Wait until either the result arrives or the test times out.
        Throwable thrown;
        try {
            thrown = getThrowable(result);
        } catch (TimeoutException e) {
            vmIsUnstable = true;
            Thread executingThread = executingThreadReference.get();
            if (executingThread != null) {
                executingThread.interrupt();
                e.setStackTrace(executingThread.getStackTrace());
            }
            thrown = e;
        }

        if (thrown != null) {
            throw thrown;
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private Throwable getThrowable(Future<Throwable> result)
            throws InterruptedException, ExecutionException, TimeoutException {
        Throwable thrown;
        thrown = timeoutSeconds == 0
                ? result.get()
                : result.get(timeoutSeconds, TimeUnit.SECONDS);
        return thrown;
    }
}
