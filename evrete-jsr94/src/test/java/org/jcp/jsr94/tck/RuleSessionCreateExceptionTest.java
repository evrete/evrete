/*
 * J A V A  C O M M U N I T Y  P R O C E S S
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck;

// java imports

import org.junit.jupiter.api.Test;

import javax.rules.RuleSessionCreateException;

/**
 * Test the javax.rules.RuleSessionCreateException class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testRuleSessionCreateException}
 * <ul>
 * <li>Instance Creation
 * <li>Class Hierarchy
 * <li>Exception Wrapping
 * </ul>
 * </ul>
 *
 * @version 1.0
 * @see javax.rules.RuleSessionCreateException
 * @since JSR-94 1.0
 */
class RuleSessionCreateExceptionTest {
    /**
     * Test the compliance for javax.rules.RuleSessionCreateException.
     *
     * <p>
     * <b>Description:</b><br>
     * Create two instances of the RuleSessionCreateException class. The first
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
     * <li>Fail: If RuleSessionCreateException cannot be created by any of
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
     * RuleSessionCreateException is unwrapped.
     * <li>Succeed: If exception can successfully be unwrapped.
     * </ul>
     * </ul>
     */
    @Test
    void testRuleSessionCreateException() {
        RuleSessionCreateException re1 = null;
        RuleSessionCreateException re2 = null;

        try {
            re1 = new RuleSessionCreateException("jsr94-test-rule-session-create-exception");
            re2 = new RuleSessionCreateException("jsr94-test-embedded-rule-session-create-exception", re1);

            // Throw it.
            throw re2;
        } catch (RuleSessionCreateException ex) {
            // Catch it.
            Throwable t = ex.getCause();

            // The cause of the exception should be re1.
            assert t.equals(re1);
            // The thrown exception should be re2.
            assert ex.equals(re2);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
