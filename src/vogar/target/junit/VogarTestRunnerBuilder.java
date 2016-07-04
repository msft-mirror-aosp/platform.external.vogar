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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

/**
 * A {@link RunnerBuilder} that will create a {@link VogarTestRunner} for a JUnit3 or JUnit4 based
 * test class.
 */
public class VogarTestRunnerBuilder extends RunnerBuilder {

    private final RunnerParams runnerParams;

    public VogarTestRunnerBuilder(RunnerParams runnerParams) {
        this.runnerParams = runnerParams;
    }

    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        Set<String> methodNames = JUnitUtils.mergeQualificationAndArgs(
                runnerParams.getQualification(), runnerParams.getArgs());
        final List<VogarTest> tests;
        if (Junit3.isJunit3Test(testClass)) {
            tests = Junit3.classToVogarTests(testClass, methodNames);
        } else if (Junit4.isJunit4Test(testClass)) {
            tests = Junit4.classToVogarTests(testClass, methodNames);
        } else {
            return null;
        }

        // Sort the tests to ensure consistent ordering.
        Collections.sort(tests, new Comparator<VogarTest>() {
            @Override
            public int compare(VogarTest o1, VogarTest o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        return new VogarTestRunner(tests, runnerParams.getTimeoutSeconds());
    }
}
