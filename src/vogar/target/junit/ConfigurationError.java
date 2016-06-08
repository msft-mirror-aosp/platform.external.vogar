/*
 * Copyright (C) 2013 The Android Open Source Project
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

import javax.annotation.Nullable;
import org.junit.runner.Description;

class ConfigurationError implements VogarTest {
    private final String className;
    @Nullable
    private final String methodName;
    private final Throwable cause;

    ConfigurationError(String className, @Nullable String methodName, Throwable cause) {
        this.className = className;
        this.methodName = methodName;
        this.cause = cause;
    }

    @Override
    public void run() throws Throwable {
        throw cause;
    }

    @Override
    public Description getDescription() {
        if (methodName == null) {
            return Description.createSuiteDescription(className);
        } else {
            String testName = String.format("%s(%s)", methodName, className);
            return Description.createSuiteDescription(testName);
        }
    }

    @Override
    public String toString() {
        return methodName == null ? className : className + "#" + methodName;
    }
}
