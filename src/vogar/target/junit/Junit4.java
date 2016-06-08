/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Utilities for manipulating JUnit4 tests.
 */
public final class Junit4 {
    private Junit4() {}

    /**
     * Creates lazy vogar test instances from the given test case or test
     * suite.
     *
     * @param methodNames if non-empty, this is the list of test method names.
     */
    static List<VogarTest> classToVogarTests(Class<?> testClass, Collection<String> methodNames) {
        List<VogarTest> result = new ArrayList<>();
        getSuiteMethods(result, testClass, methodNames);
        return result;
    }

    private static void getSuiteMethods(
        List<VogarTest> out, Class<?> testClass, Collection<String> methodNames) {

        // Try the suites first, if we find one then exit as suites take priority.
        boolean isJunit4TestClass = getSuiteTests(out, testClass);
        if (isJunit4TestClass) {
            return;
        }

        Collection<Object[]> argCollection = findParameters(testClass);

        /* JUnit 4.x: methods marked with @Test annotation. */
        if (methodNames.isEmpty()) {
            for (Method m : testClass.getMethods()) {
                if (!m.isAnnotationPresent(org.junit.Test.class)) continue;

                isJunit4TestClass = true;

                addTest(out, testClass, m, argCollection);
            }
        } else {
            for (String methodName : methodNames) {
                try {
                    Method m = testClass.getMethod(methodName);
                    if (!m.isAnnotationPresent(org.junit.Test.class)) continue;

                    isJunit4TestClass = true;

                    addTest(out, testClass, m, argCollection);
                } catch (final NoSuchMethodException e) {
                    // Check to see if any method with the specified name exists.
                    Throwable cause = null;
                    for (Method m : testClass.getMethods()) {
                        if (m.getName().equals(methodName)) {
                            cause = new Exception("Method " + methodName
                                    + " should have no parameters");
                            break;
                        }
                    }
                    if (cause == null) {
                        cause = new AssertionFailedError("Method \"" + methodName + "\" not found");
                    }
                    ConfigurationError error = new ConfigurationError(
                            testClass.getName(), methodName, cause);
                    out.add(error);

                    // An error happened so just treat this as a JUnit4 test otherwise it will end
                    // up producing another error below.
                    isJunit4TestClass = true;
                }
            }
        }

        if (!isJunit4TestClass) {
            out.add(new ConfigurationError(testClass.getName(), null,
                    new IllegalStateException("Not a test case: " + testClass)));
        }
    }

    private static void addTest(List<VogarTest> out, Class<?> testClass, Method m, Collection<Object[]> argCollection) {
        if (m.isAnnotationPresent(Ignore.class)) {
            // Ignore the test.
        } else if (m.getParameterTypes().length == 0) {
            addAllParameterizedTests(out, testClass, m, argCollection);
        } else {
            String name = m.getName();
            out.add(new ConfigurationError(testClass.getName(), name,
                    new Exception("Method " + name + " should have no parameters")));
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object[]> findParameters(Class<?> testClass) {
        for (Method m : testClass.getMethods()) {
            for (Annotation a : m.getAnnotations()) {
                if (Parameters.class.isAssignableFrom(a.annotationType())) {
                    try {
                        return (Collection<Object[]>) m.invoke(testClass);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return null;
    }

    private static void addAllParameterizedTests(List<VogarTest> out, Class<?> testClass, Method m,
            Collection<Object[]> argCollection) {
        if (argCollection == null) {
            out.add(TestMethod.create(testClass, m, null));
        } else {
            for (Object[] args : argCollection) {
                out.add(TestMethod.create(testClass, m, args));
            }
        }
    }

    public static boolean isJunit4Test(Class<?> klass) {
        boolean isTestSuite = false;
        boolean hasSuiteClasses = false;

        boolean concrete = !Modifier.isAbstract(klass.getModifiers());

        // @RunWith(Suite.class)
        // @SuiteClasses( ... )
        // public class MyTest { ... }
        //   or
        // @RunWith(Parameterized.class)
        // public class MyTest { ... }
        for (Annotation a : klass.getAnnotations()) {
            Class<?> annotationClass = a.annotationType();

            if (RunWith.class.isAssignableFrom(annotationClass)) {
                Class<?> runnerClass = ((RunWith) a).value();
                if (Suite.class.isAssignableFrom(runnerClass)) {
                    isTestSuite = true;
                } else if (Parameterized.class.isAssignableFrom(runnerClass)) {
                    // @Parameterized test classes are instantiated so must be concrete.
                    return concrete;
                }
            } else if (Suite.SuiteClasses.class.isAssignableFrom(annotationClass)) {
                hasSuiteClasses = true;
            }

            if (isTestSuite && hasSuiteClasses) {
                // This doesn't instantiate the class so doesn't care if it's abstract.
                return true;
            }
        }

        // Test classes that have methods annotated with @Test are instantiated so must be concrete.
        if (!concrete) {
            return false;
        }

        // public class MyTest {
        //     @Test
        //     public void example() { ... }
        // }
        for (Method m : klass.getDeclaredMethods()) {
            for (Annotation a : m.getAnnotations()) {
                if (org.junit.Test.class.isAssignableFrom(a.annotationType())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean getSuiteTests(List<VogarTest> out, Class<?> suite) {
        boolean isSuite = false;

        /* Check for @RunWith(Suite.class) */
        for (Annotation a : suite.getAnnotations()) {
            if (RunWith.class.isAssignableFrom(a.annotationType())) {
                if (Suite.class.isAssignableFrom(((RunWith) a).value())) {
                    isSuite = true;
                }
                break;
            }
        }

        if (!isSuite) {
            return false;
        }

        /* Extract classes to run */
        for (Annotation a : suite.getAnnotations()) {
            if (SuiteClasses.class.isAssignableFrom(a.annotationType())) {
                for (Class<?> clazz : ((SuiteClasses) a).value()) {
                    getSuiteMethods(out, clazz, Collections.<String>emptySet());
                }
            }
        }

        return true;
    }

    private abstract static class VogarJUnitTest implements VogarTest {
        protected final Class<?> testClass;
        protected final Method method;

        protected VogarJUnitTest(Class<?> testClass, Method method) {
            this.testClass = testClass;
            this.method = method;
        }

        public void run() throws Throwable {
            Object testCase = getTestCase();
            Throwable failure = null;

            try {
                Class.forName("org.mockito.MockitoAnnotations")
                        .getMethod("initMocks", Object.class)
                        .invoke(null, testCase);
            } catch (Exception ignored) {
            }

            try {
                invokeMethodWithAnnotation(testCase, BeforeClass.class);
                invokeMethodWithAnnotation(testCase, Before.class);
                method.invoke(testCase);
            } catch (InvocationTargetException t) {
                failure = t.getCause();
            } catch (Throwable t) {
                failure = t;
            }

            try {
                invokeMethodWithAnnotation(testCase, After.class);
            } catch (InvocationTargetException t) {
                if (failure == null) {
                    failure = t.getCause();
                }
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
            }

            try {
                invokeMethodWithAnnotation(testCase, AfterClass.class);
            } catch (InvocationTargetException t) {
                if (failure == null) {
                    failure = t.getCause();
                }
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
            }

            if (!meetsExpectations(failure, method)) {
                if (failure == null) {
                    throw new AssertionFailedError("Expected exception not thrown");
                } else {
                    throw failure;
                }
            }
        }

        private void invokeMethodWithAnnotation(Object testCase, Class<?> annotation)
                throws IllegalAccessException, InvocationTargetException {
            for (Method m : testCase.getClass().getDeclaredMethods()) {
                for (Annotation a : m.getAnnotations()) {
                    if (annotation.isAssignableFrom(a.annotationType())) {
                        m.invoke(testCase);
                    }
                }
            }
        }

        protected boolean meetsExpectations(Throwable failure, Method method) {
            Class<?> expected = null;
            for (Annotation a : method.getAnnotations()) {
                if (org.junit.Test.class.isAssignableFrom(a.annotationType())) {
                    expected = ((org.junit.Test) a).expected();
                }
            }
            return expected == null || org.junit.Test.None.class.isAssignableFrom(expected)
                    ? (failure == null)
                    : (failure != null && expected.isAssignableFrom(failure.getClass()));
        }

        protected abstract Object getTestCase() throws Throwable;

        @Override
        public Description getDescription() {
            return Description.createTestDescription(testClass, method.getName());
        }

        @Override public String toString() {
            return testClass.getName() + "#" + method.getName();
        }
    }

    /**
     * A JUnit TestCase constructed on demand and then released.
     */
    private static class TestMethod extends VogarJUnitTest {
        private final Constructor<?> constructor;
        private final Object[] constructorArgs;

        private TestMethod(Class<?> testClass, Method method,
                Constructor<?> constructor, Object[] constructorArgs) {
            super(testClass, method);
            this.constructor = constructor;
            this.constructorArgs = constructorArgs;
        }

        public static VogarTest create(Class<?> testClass, Method method,
                Object[] constructorArgs) {
            if (constructorArgs != null) {
                for (Constructor<?> c : testClass.getConstructors()) {
                    if (c.getParameterTypes().length == constructorArgs.length) {
                        return new TestMethod(testClass, method, c, constructorArgs);
                    }
                }

                return new ConfigurationError(testClass.getName(), method.getName(),
                        new Exception("Parameterized test cases must have "
                                + constructorArgs.length + " arg constructor"));
            }

            try {
                return new TestMethod(testClass, method, testClass.getConstructor(), null);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return new TestMethod(testClass, method,
                        testClass.getConstructor(String.class), new Object[] { method.getName() });
            } catch (NoSuchMethodException ignored) {
            }

            return new ConfigurationError(testClass.getName(), method.getName(),
                    new Exception("Test cases must have a no-arg or string constructor."));
        }

        @Override protected Object getTestCase() throws Throwable {
            try {
                return constructor.newInstance(constructorArgs);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
