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
import vogar.Result;
import vogar.monitor.TargetMonitor;
import vogar.target.Profiler;
import vogar.target.TestEnvironment;
import vogar.util.Threads;

/**
 * A {@link org.junit.runner.Runner} that can run a list of {@link VogarTest} instances.
 */
public class VogarTestRunner extends ParentRunner<VogarTest> {

    private final List<VogarTest> children;
    private final TargetMonitor monitor;

    private final TestEnvironment testEnvironment;
    private final int timeoutSeconds;

    private final ExecutorService executor = Executors.newCachedThreadPool(
            Threads.daemonThreadFactory("testrunner"));
    private final Profiler profiler;

    private boolean vmIsUnstable;

    public VogarTestRunner(List<VogarTest> children, TargetMonitor monitor,
                           TestEnvironment testEnvironment, int timeoutSeconds,
                           Profiler profiler)
            throws InitializationError {
        super(VogarTestRunner.class);
        this.children = children;
        this.monitor = monitor;
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
    private void runWithTimeout(final VogarTest test) {
        testEnvironment.reset();
        monitor.outcomeStarted(test.toString());

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
        } catch (Exception e) {
            thrown = e;
        }

        if (thrown != null) {
            prepareForDisplay(thrown);
            thrown.printStackTrace(System.out);
            monitor.outcomeFinished(Result.EXEC_FAILED);
        } else {
            monitor.outcomeFinished(Result.SUCCESS);
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

    /**
     * Strip vogar's lines from the stack trace. For example, we'd strip the
     * first two Assert lines and everything after the testFoo() line in this
     * stack trace:
     *
     *   at junit.framework.Assert.fail(Assert.java:198)
     *   at junit.framework.Assert.assertEquals(Assert.java:56)
     *   at junit.framework.Assert.assertEquals(Assert.java:61)
     *   at libcore.java.net.FooTest.testFoo(FooTest.java:124)
     *   at java.lang.reflect.Method.invokeNative(Native Method)
     *   at java.lang.reflect.Method.invoke(Method.java:491)
     *   at vogar.target.junit.Junit$JUnitTest.run(Junit.java:214)
     *   at vogar.target.junit.JUnitRunner$1.call(JUnitRunner.java:112)
     *   at vogar.target.junit.JUnitRunner$1.call(JUnitRunner.java:105)
     *   at java.util.concurrent.FutureTask$Sync.innerRun(FutureTask.java:305)
     *   at java.util.concurrent.FutureTask.run(FutureTask.java:137)
     *   at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1076)
     *   at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:569)
     *   at java.lang.Thread.run(Thread.java:863)
     */
    public void prepareForDisplay(Throwable t) {
        StackTraceElement[] stackTraceElements = t.getStackTrace();
        boolean foundVogar = false;

        int last = stackTraceElements.length - 1;
        for (; last >= 0; last--) {
            String className = stackTraceElements[last].getClassName();
            if (className.startsWith("vogar.target")) {
                foundVogar = true;
            } else if (foundVogar
                    && !className.startsWith("java.lang.reflect")
                    && !className.startsWith("sun.reflect")
                    && !className.startsWith("junit.framework")) {
                if (last < stackTraceElements.length) {
                    last++;
                }
                break;
            }
        }

        int first = 0;
        for (; first < last; first++) {
            String className = stackTraceElements[first].getClassName();
            if (!className.startsWith("junit.framework")) {
                break;
            }
        }

        if (first > 0) {
            first--; // retain one assertSomething() line in the trace
        }

        if (first < last) {
            // Arrays.copyOfRange() didn't exist on Froyo
            StackTraceElement[] copyOfRange = new StackTraceElement[last - first];
            System.arraycopy(stackTraceElements, first, copyOfRange, 0, last - first);
            t.setStackTrace(copyOfRange);
        }
    }
}
