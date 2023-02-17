/*
 * JAVA COMMUNITY PROCESS
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck;

// java imports

import org.junit.jupiter.api.Test;

import javax.rules.ConfigurationException;

/**
 * Test the javax.rules.ConfigurationException class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testConfigurationException}
 * <ul>
 * <li>Instance Creation
 * <li>Class Hierarchy
 * <li>Exception Wrapping
 * </ul>
 * </ul>
 *
 * @version 1.0
 * @see javax.rules.ConfigurationException
 * @since JSR-94 1.0
 */
class ConfigurationExceptionTest {

    /**
     * Test the compliance for javax.rules.ConfigurationException.
     *
     * <p>
     * <b>Description:</b><br>
     * Create two instances of the ConfigurationException. The first
     * instance will be constructed with an additional message. The
     * second instance will have its own message as well as wrap the
     * first instance. The class hierarchy will be tested. The second
     * exception will be thrown. The exception will be caught and the
     * test will verify whether the exception can successfully be
     * unwrapped.
     * The following tests will be performed.
     * <ul>
     * <li>Instance Creation
     * <ul>
     * <li>Fail: If ConfigurationException cannot be created by any of
     * the JSR specified constructor.
     * <li>Succeed: If the exception can successfully be created.
     * </ul>
     * <li>Class Hierarchy
     * <ul>
     * <li>Fail: If superclass is not a RuleException
     * <li>Succeed: If the exception is instance of RuleException.
     * </ul>
     * <li>Exception Wrapping
     * <ul>
     * <li>Fail: If any other than the the original
     * ConfigurationException is unwrapped.
     * <li>Succeed: If exception can successfully be unwrapped.
     * </ul>
     * </ul>
     */
    @Test
    void testConfigurationException() {
        ConfigurationException ce1 = null;
        ConfigurationException ce2 = null;

        try {
            ce1 = new ConfigurationException(
                    "jsr94-test-configuration-exception");
            ce2 = new ConfigurationException(
                    "jsr94-test-embedded-configuration-exception", ce1);


            // Throw it.
            throw ce2;
        } catch (ConfigurationException ex) {
            // Catch it.
            String s = ex.getMessage();
            Throwable t = ex.getCause();

            // The cause of the exception should be ce1.
            assert t.equals(ce1);

            // The thrown exception should be ce2.
            assert ex.equals(ce2);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
