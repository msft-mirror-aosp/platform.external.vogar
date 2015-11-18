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

import com.google.caliper.SimpleBenchmark;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import vogar.monitor.TargetMonitor;

/**
 * Supports treating a class as a Caliper benchmark.
 */
public class CaliperRunnerFactory implements RunnerFactory {

    @Override @Nullable
    public Runner newRunner(TargetMonitor monitor, String actionName, String qualification,
            Class<?> klass, AtomicReference<String> skipPastReference,
            TestEnvironment testEnvironment, int timeoutSeconds, boolean profile) {
        if (SimpleBenchmark.class.isAssignableFrom(klass)) {
            return new CaliperRunner(monitor, profile, klass);
        } else {
            return null;
        }
    }
}