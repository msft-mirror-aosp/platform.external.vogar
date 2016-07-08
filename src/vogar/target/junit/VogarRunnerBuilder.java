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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

/**
 * A composite {@link RunnerBuilder} that will ask each of its list of {@link RunnerBuilder} to
 * create a runner, returning the result of the first that does so, or null if none match.
 */
public class VogarRunnerBuilder extends RunnerBuilder {

    private final Collection<RunnerBuilder> builders;

    public VogarRunnerBuilder(RunnerParams runnerParams) {
        builders = new ArrayList<>();
        builders.add(new VogarTestRunnerBuilder(runnerParams) {
            @Override
            public List<VogarTest> getVogarTests(Class<?> testClass, Set<String> methodNames) {
                if (Junit3.isJunit3Test(testClass)) {
                    return Junit3.classToVogarTests(testClass, methodNames);
                } else {
                    return null;
                }
            }
        });
        builders.add(new VogarTestRunnerBuilder(runnerParams) {
            @Override
            public List<VogarTest> getVogarTests(Class<?> testClass, Set<String> methodNames) {
                if (Junit4.isJunit4Test(testClass)) {
                    return Junit4.classToVogarTests(testClass, methodNames);
                } else {
                    return null;
                }
            }
        });
    }

    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        for (RunnerBuilder builder : builders) {
            Runner runner = builder.safeRunnerForClass(testClass);
            if (runner != null) {
                return runner;
            }
        }

        return null;
    }

}
