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
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * A {@link org.junit.runner.Runner} that can run a list of {@link VogarTest} instances.
 */
public class VogarTestRunner extends ParentRunner<VogarTest> {

    private final List<VogarTest> children;

    public VogarTestRunner(List<VogarTest> children) throws InitializationError {
        super(VogarTestRunner.class);
        this.children = children;
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
                child.run();
            }
        }, describeChild(child), notifier);
    }
}
