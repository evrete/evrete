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

import org.jcp.jsr94.tck.util.TestCaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.rules.RuleExecutionSetMetadata;
import javax.rules.StatelessRuleSession;

/**
 * Test the javax.rules.RuleExecutionSetMetadata class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testRuleExecutionSetMetadata}
 * <ul>
 * <li>Retrieve Metadata
 * <li>Verify URI
 * <li>Verify name
 * </ul>
 * </ul>
 * <b>Note:</b><br>
 * This test uses a RuleSetExecution definition that is stored in the
 * tck_res_1.xml. You have to change this file according to your rule
 * execution set definition. The mainline TCK documentation describes
 * the contents of this rule execution set definition.
 *
 * @version 1.0
 * @see javax.rules.RuleExecutionSetMetadata
 * @since JSR-94 1.0
 */
class RuleExecutionSetMetadataTest {
    private final static String RULE_EXECUTION_SET_URI = "RuleExecutionSetMetadataTest";
    // Stateless rule session.
    private StatelessRuleSession ruleSession;

    @BeforeEach
    void setUp() {
        try {
            ruleSession = TestCaseUtil.getStatelessRuleSession(
                    RULE_EXECUTION_SET_URI, "src/test/resources/TckRes1.java");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterEach
    void tearDown() {
        ruleSession = null;
    }

    /**
     * Test the compliance for javax.rules.RuleExecutionSetMetadata.
     *
     * <p>
     * <b>Description:</b><br>
     * Create a stateless rule session and retrieve the rule execution
     * set metadata. Determine the name and uri of this metadata.
     * <ul>
     * <li>Retrieve Metadata
     * <ul>
     * <li>Fail: If the rule execution set metadata cannot be retrieved
     * from the rule session.
     * <li>Succeed: If a non null RuleExecutionSetMetadata is retrieved.
     * </ul>
     * <li>Verify URI
     * <ul>
     * <li>Fail: If uri is not the same as specified during the binding
     * of the rule execution set.
     * <li>Succeed: If uri corresponds with the internal given binding.
     * </ul>
     * <li>Verify name
     * <ul>
     * <li>Fail: If name is null.
     * <li>Succeed: If the metadata has a non null name.
     * </ul>
     * </ul>
     *
     * @see TestCaseUtil#getStatelessRuleSession
     */
    @Test
    void testRuleExecutionSetMetadata() {
        try {
            RuleExecutionSetMetadata data =
                    ruleSession.getRuleExecutionSetMetadata();

            // Check that we have a metadata.
            assert data != null;

            // Check whether the uri is the same as the uri given
            // during the creation of the session.
            assert RULE_EXECUTION_SET_URI.equals(data.getUri());

            // Check whether the rule execution set has a name.
            assert data.getName() != null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
