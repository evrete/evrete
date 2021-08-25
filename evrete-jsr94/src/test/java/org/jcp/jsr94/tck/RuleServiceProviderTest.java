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
import org.jcp.jsr94.tck.util.TestRuleServiceProvider;
import org.junit.jupiter.api.Test;

import javax.rules.RuleServiceProvider;

/**
 * Test the javax.rules.RuleServiceProvider class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testRuleServiceProvider}
 * <ul>
 * <li>Create Instance
 * <li>Retrieve RuleAdministrator
 * <li>Retrieve RuleRuntime
 * </ul>
 * </ul>
 *
 * @version 1.0
 * @see javax.rules.RuleServiceProvider
 * @since JSR-94 1.0
 */
class RuleServiceProviderTest {
    /**
     * Test the compliance for javax.rules.RuleServiceProvider.
     * This test will retrieve the rule engine vendor specific rule
     * service provider class name from the tck.conf configuration
     * file. An instance will be created via the static createInstance
     * method of the RuleServiceProvider class. The rule administrator
     * and rule runtime APIs will be tested.
     * <p>
     * <b>Description:</b><br>
     * <ul>
     * <li>Create Instance
     * <ul>
     * <li>Fail: If any exception occurs during the creation of the
     * rule service provider.
     * <li>Succeed: If the rule service provider can successfully be
     * instantiated.
     * </ul>
     * <li>Retrieve RuleAdministrator
     * <ul>
     * <li>Fail: If any exception occurs during the creation of the
     * rule service provider.
     * <li>Succeed: If the rule service provider can successfully be
     * instantiated.
     * </ul>
     * <li>Retrieve RuleRuntime
     * <ul>
     * <li>Fail: If any exception occurs during the creation of the
     * rule service provider.
     * <li>Succeed: If the rule service provider can successfully be
     * instantiated.
     * </ul>
     * </ul>
     *
     * @see TestCaseUtil#getRuleServiceProvider
     * @see TestRuleServiceProvider#createInstance
     */
    @Test
    void testRuleServiceProvider() {
        try {
            // Get the name of the vendor specific RuleServiceProvider.
            // as specified in the tck.conf configuration file.
            String providerName = TestCaseUtil.getRuleServiceProvider();
            assert providerName != null;
            // Create the provider.
            Object obj = TestRuleServiceProvider.createInstance(providerName);

            // Check whether or not it is indeed a rule service
            // provider.
            assert obj != null;
            assert obj instanceof RuleServiceProvider;

            RuleServiceProvider provider = (RuleServiceProvider) obj;

            // Test the API.

            // Get the rule administrator.
            obj = provider.getRuleAdministrator();

            // Check whether or not it is indeed a rule administrator.
            assert obj != null;

            // Get the rule runtime.
            obj = provider.getRuleRuntime();

            // Check whether or not it is indeed a rule runtime.
            assert obj != null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
