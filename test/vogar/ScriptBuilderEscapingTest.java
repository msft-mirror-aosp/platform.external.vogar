package vogar;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import vogar.Target.ScriptBuilder;

/**
 * Tests the {@link ScriptBuilder#escape(String)} method.
 */
public class ScriptBuilderEscapingTest {

    public static Test suite() {
        TestSuite suite = new TestSuite(ScriptBuilderEscapingTest.class.getName());
        char[] chars = " '\"<>&|$".toCharArray();
        for (char c : chars) {
            suite.addTest(new SingleCharacterEscapeTest(c));
        }
        suite.addTest(new MixedCharacterEscapeTest());
        return suite;
    }

    private static class SingleCharacterEscapeTest extends TestCase {

        private final String uc;
        private final String qc;

        public SingleCharacterEscapeTest(char c) {
            this.uc = Character.toString(c);
            this.qc = "\\" + c;
            setName("Escape '" + uc + "' as '" + qc + "'");
        }

        @Override
        protected void runTest() throws Throwable {
            assertEquals(qc, ScriptBuilder.escape(uc));
            assertEquals("a" + qc, ScriptBuilder.escape("a" + uc));
            assertEquals(qc + "b", ScriptBuilder.escape(uc + "b"));
            assertEquals("a" + qc + "b", ScriptBuilder.escape("a" + uc + "b"));
            assertEquals(qc + "a" + qc + qc + qc + "b" + qc,
                    ScriptBuilder.escape(uc + "a" + uc + uc + uc + "b" + uc));

        }
    }

    private static class MixedCharacterEscapeTest extends TestCase {

        public MixedCharacterEscapeTest() {
            super("mixed character escape test");
        }

        @Override
        protected void runTest() throws Throwable {
            assertEquals("\\ \\'\\\"\\<\\>\\&\\|\\$",
                    ScriptBuilder.escape(" '\"<>&|$"));
        }
    }
}