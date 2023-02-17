/*
 * JAVA COMMUNITY PROCESS
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck.admin;

// java imports

import org.jcp.jsr94.tck.util.TestCaseUtil;
import org.junit.jupiter.api.Test;

import javax.rules.RuleServiceProvider;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetProvider;
import java.io.File;
import java.util.HashMap;

/**
 * Test the javax.rules.admin.RuleExecutionSetProvider class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testRuleExecutionSetProvider}
 * <ul>
 * <li>Create Instance
 * <li>RuleExecutionSet form a Document element
 * <li>RuleExecutionSet form a uri
 * </ul>
 * </ul>
 * <b>Note:</b><br>
 * See the TCK documentation for more information on the rule execution
 * set definition that is used for testing this provider API. The
 * actual loading of the rule execution set is handled by the
 * TestCaseUtil class file.
 *
 * @version 1.0
 * @see javax.rules.admin.RuleExecutionSetProvider
 * @since JSR-94 1.0
 */
class RuleExecutionSetProviderTest {

    /**
     * Test the compliance for javax.rules.admin.RuleExecutionSetProvider.
     * Get the rule engine vendor specific implementation of the
     * RuleExecutionSetProvider via the RuleServiceProvider
     * specified in the tck.conf configuration file. Get the
     * RuleAdministrator from this service provider which will have a
     * reference to a RuleExecutionSetProvider. The test will
     * continue with constructing a RuleExecutionSet from a
     * String (uri) and a DOM Document element.
     * In both cases the "tck_res_1.xml" rule execution set definition
     * will be used as input.
     *
     * <p>
     * <b>Description:</b><br>
     * <ul>
     * <li>Create Instance
     * <ul>
     * <li>Fail: If any errors occur when trying to get a reference to
     * the RuleExecutionSetProvider.
     * <li>Succeed: If a reference to an instance of the
     * RuleExecutionSetProvider is successfully obtained.
     * </ul>
     * <li>Document element
     * <ul>
     * <li>Fail: If any errors occur during the creation of a
     * RuleExecutionSet from a DOM Document element.
     * <li>Succeed: If a RuleExecutionSet can successfully be created
     * from a DOM Document element.
     * </ul>
     * <li>String (uri) (currently disabled, as the TCK does not know the
     * format of the rule engine's URIs). review DCS - this should be externalized.
     * <ul>
     * <li>Fail: If any errors occur during the creation of a
     * RuleExecutionSet from a URI.
     * <li>Succeed: If a RuleExecutionSet can successfully be created
     * from a URI.
     * </ul>
     * </ul>
     */
    @Test
    void testRuleExecutionSetProvider() {
        try {
            // Get the RuleServiceProvider
            RuleServiceProvider serviceProvider =
                    TestCaseUtil.getRuleServiceProvider("ResTest");
            assert serviceProvider != null;

            // Get the RuleAdministrator
            RuleAdministrator ruleAdministrator = serviceProvider.getRuleAdministrator();
            assert ruleAdministrator != null;

            // Get the RuleExecutionSetProvider
            RuleExecutionSetProvider provider = ruleAdministrator.getRuleExecutionSetProvider(null);
            assert provider != null;

            // Now test the API.
            RuleExecutionSet res;

            HashMap<Object, Object> props = new HashMap<>();
            props.put("org.evrete.jsr94.dsl-name", "JAVA-SOURCE");

            File f = new File("src/test/resources/TckRes1.java");
            res = provider.createRuleExecutionSet(f, props);
            assert res != null : "[RuleExecutionSetProviderTest] Could not created RuleExecutionSet from a DOM Document element";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
