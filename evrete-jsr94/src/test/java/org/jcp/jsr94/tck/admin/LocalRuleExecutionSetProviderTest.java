/*
 * J A V A  C O M M U N I T Y  P R O C E S S
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
import javax.rules.admin.LocalRuleExecutionSetProvider;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;

/**
 * Test the javax.rules.admin.LocalRuleExecutionSetProvider class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testLocalRuleExecutionSetProvider}
 * <ul>
 * <li>Create Instance
 * <li>InputStream
 * <li>Reader
 * </ul>
 * </ul>
 * <b>Note:</b><br>
 * See the TCK documentation for more information on the rule execution
 * set definition that is used for testing this provider API. The
 * actual loading of the rule execution set is handled by the
 * TestCaseUtil class file.
 *
 * @version 1.0
 * @see javax.rules.admin.LocalRuleExecutionSetProvider
 * @see TestCaseUtil#getRuleExecutionSetInputStream
 * @see TestCaseUtil#getRuleExecutionSetReader
 * @since JSR-94 1.0
 */
class LocalRuleExecutionSetProviderTest {

    /**
     * Test the compliance for javax.rules.admin.LocalRuleExecutionSetProvider.
     * Get the rule engine vendor specific implementation of the
     * LocalRuleExecutionSetProvider via the RuleServiceProvider
     * specified in the tck.conf configuration file. Get the
     * RuleAdministrator from this service provider which will have a
     * reference to a LocalRuleExecutionSetProvider. The test will
     * continue with constructing a RuleExecutionSet from an
     * InputStream and a Reader. In both cases the "tck_res_1.xml" rule
     * execution set definition will be used as input.
     * <p>
     * <b>Description:</b><br>
     * <ul>
     * <li>Create Instance
     * <ul>
     * <li>Fail: If any errors occur when trying to get a reference to
     * the LocalRuleExecutionSetProvider.
     * <li>Succeed: If a reference to an instance of the
     * LocalRuleExecutionSetProvider is successfully obtained.
     * </ul>
     * <li>InputStream
     * <ul>
     * <li>Fail: If any errors occur during the creation of a
     * RuleExecutionSet from an InputStream.
     * <li>Succeed: If a RuleExecutionSet can successfully be created
     * from an InputStream.
     * </ul>
     * <li>Reader
     * <ul>
     * <li>Fail: If any errors occur during the creation of a
     * RuleExecutionSet from a Reader.
     * <li>Succeed: If a RuleExecutionSet can successfully be created
     * from a Reader.
     * </ul>
     * </ul>
     *
     * @see TestCaseUtil#getRuleExecutionSetInputStream
     * @see TestCaseUtil#getRuleExecutionSetReader
     */
    @Test
    void testLocalRuleExecutionSetProvider() throws Exception {
        // Get the RuleServiceProvider
        RuleServiceProvider serviceProvider =
                TestCaseUtil.getRuleServiceProvider("LocalResTest");
        assert serviceProvider != null : "[localRuleExecutionSetProviderTest] RuleServiceProvider not found.";

        // Get the RuleAdministrator
        RuleAdministrator ruleAdministrator =
                serviceProvider.getRuleAdministrator();
        assert ruleAdministrator != null;

        // Get the LocalRuleExecutionSetProvider
        LocalRuleExecutionSetProvider localProvider = ruleAdministrator.getLocalRuleExecutionSetProvider(null);
        assert localProvider != null;

        // Now test the API.
        RuleExecutionSet res;

        HashMap<Object, Object> props = new HashMap<>();
        props.put("org.evrete.jsr94.dsl-name", "JAVA-SOURCE");
        InputStream inStream = TestCaseUtil.getRuleExecutionSetInputStream("src/test/resources/TckRes1.java");
        res = localProvider.createRuleExecutionSet(inStream, props);
        assert res != null;

        Reader reader = TestCaseUtil.getRuleExecutionSetReader("src/test/resources/TckRes2.java");
        res = localProvider.createRuleExecutionSet(reader, props);
        assert res != null;
    }
}
