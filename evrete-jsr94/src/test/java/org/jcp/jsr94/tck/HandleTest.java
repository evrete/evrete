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

import org.jcp.jsr94.tck.model.Customer;
import org.jcp.jsr94.tck.util.TestCaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.rules.Handle;
import javax.rules.StatefulRuleSession;

/**
 * Tests the compliance of {@link javax.rules.Handle} implementation classes.
 * This class performs the following test scenario:
 * <ul>
 * <li> An object is added to a stateful rule session and a handle is obtained.
 *      That object has defined properly the <code>equals</code> and
 *      <code>hashCode</code> methods.
 * <li> The obtained handle must be an instance of <code>Handle</code>.
 * <li> The rule session then must contain the handle.
 * <li> The object returned by {@link StatefulRuleSession#getObject(Handle)} must be
 *      equals to the initial object.
 * <li> The object is changed to be another object of the same class.
 * <li> The rule session is updated with the handle containing the new object.
 * <li> The rule session must again contain the handle.
 * <li> The object returned by {@link StatefulRuleSession#getObject(Handle)} must be
 *      equals to this new object.
 * <li> The handle is removed from the rule session.
 * <li> The rule session must not contain the handle.
 * </ul>
 * <b>Note:</b><br>
 * This test uses a RuleSetExecution definition that is stored in the
 * tck_res_2.xml. You have to change this file according to your rule
 * execution set definition. The mainline TCK documentation describes
 * the contents of this rule execution set definition.
 *
 * @version 1.0
 * @see javax.rules.Handle
 * @since JSR-94 1.0
 */
class HandleTest {
    // Stateful rule session.
    private StatefulRuleSession ruleSession;


    /**
     * Initialize the HandleTest.
     * Initializes a stateful rule session. This test uses the
     * tck_res_2.xml RuleExecutionSet definition file.
     */
    @BeforeEach
    void setUp() {
        try {
            ruleSession = TestCaseUtil.getStatefulRuleSession("handleTest",
                    "src/test/resources/TckRes1.java");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Cleanup the HandleTest.
     */
    @AfterEach
    void tearDown() {
        ruleSession = null;
    }

    /**
     * A test for <code>javax.rules.Handle</code> implementation classes.
     */
    @Test
    void testHandle() throws Exception {
        Object object = new Customer("a customer");

        Handle handle = ruleSession.addObject(object);

        assert ruleSession.containsObject(handle);

        Object newObject = ruleSession.getObject(handle);
        assert object.equals(newObject);

        newObject = new Customer("another customer");
        ruleSession.updateObject(handle, newObject);

        assert ruleSession.containsObject(handle);

        assert newObject.equals(ruleSession.getObject(handle));

        ruleSession.removeObject(handle);
        assert !ruleSession.containsObject(handle);
    }
}
