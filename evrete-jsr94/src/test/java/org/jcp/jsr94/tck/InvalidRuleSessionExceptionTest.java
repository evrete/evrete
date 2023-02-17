/*
 * JAVA COMMUNITY PROCESS
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck;

import org.junit.jupiter.api.Test;

import javax.rules.InvalidRuleSessionException;

/**
 * Test the javax.rules.InvalidRuleSessionException class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testInvalidRuleSessionException}
 * <ul>
 * <li>Instance Creation
 * <li>Class Hierarchy
 * <li>Exception Wrapping
 * </ul>
 * </ul>
 *
 * @version 1.0
 * @see javax.rules.InvalidRuleSessionException
 * @since JSR-94 1.0
 */
class InvalidRuleSessionExceptionTest {

    /**
     * Test the compliance for javax.rules.InvalidRuleSessionException.
     *
     * <p>
     * <b>Description:</b><br>
     * Create two instances of the InvalidRuleSessionException. The first
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
     * <li>Fail: If InvalidRuleSessionException cannot be created by any of
     * the JSR specified constructors.
     * <li>Succeed: If the exception can successfully be created.
     * </ul>
     * <li>Class Hierarchy
     * <ul>
     * <li>Fail: If superclass is not a RuleExecutionException
     * <li>Succeed: If the exception is instance of RuleExecutionException.
     * </ul>
     * <li>Exception Wrapping
     * <ul>
     * <li>Fail: If any other than the the original
     * InvalidRuleSessionException is unwrapped.
     * <li>Succeed: If exception can successfully be unwrapped.
     * </ul>
     * </ul>
     */
    @Test
    void testInvalidRuleSessionException() {
        InvalidRuleSessionException ie1 = null;
        InvalidRuleSessionException ie2 = null;

        try {
            ie1 = new InvalidRuleSessionException(
                    "jsr94-test-invalid-rule-session-exception");
            ie2 = new InvalidRuleSessionException(
                    "jsr94-test-embedded-invalid-rule-session-exception", ie1);

            // Throw it.
            throw ie2;
        } catch (InvalidRuleSessionException ex) {
            // Catch it.
            Throwable t = ex.getCause();

            // The cause of the exception should be ie1.
            assert t.equals(ie1);
            // The thrown exception should be ie2.
            assert ex.equals(ie2);
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }
}
