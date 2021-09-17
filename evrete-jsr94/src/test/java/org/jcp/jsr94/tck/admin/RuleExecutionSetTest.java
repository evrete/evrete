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
import org.jcp.jsr94.tck.util.TestObjectFilter;
import org.junit.jupiter.api.Test;

import javax.rules.ObjectFilter;
import javax.rules.RuleServiceProvider;
import javax.rules.admin.LocalRuleExecutionSetProvider;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Test the javax.rules.admin.RuleExecutionSet class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testRuleExecutionSet}
 * <ul>
 * <li>Create Instance (with and without properties)
 * <li>Get/Set properties
 * <li>Get/Set object filters
 * <li>Retrieve Rules from the RuleExecutionSet
 * </ul>
 * </ul>
 * <b>Note:</b><br>
 * The tck_res_1.xml rule execution set is used for testing. For more
 * information on this rule execution set, see the TCK documentation.
 *
 * @version 1.0
 * @see javax.rules.admin.RuleExecutionSet
 * @see TestCaseUtil#getRuleExecutionSetInputStream
 * @since JSR-94 1.0
 */
class RuleExecutionSetTest {

    /**
     * Test the compliance for javax.rules.admin.RuleExecutionSet.
     * Get a RuleAdministrator from a RuleServiceProvider and get the
     * LocalRuleExecutionSetProvider from the Administrator. Create a
     * RuleExecutionSet via the LocalRuleExecutionSetProvider. An input
     * stream to the tck_res_1.xml rule execution set is used to
     * construct the rule execution set.
     *
     * <p>
     * <b>Description:</b><br>
     * <ul>
     * <li>Create Instance (with and without properties)
     * <ul>
     * <li>Fail: If any errors occur during the creation of the rule
     * execution set.
     * <li>Succeed: If an instance of a RuleExecutionSet could
     * successfully be created and the specified properties are
     * available.
     * </ul>
     * <li>Get/Set properties
     * <ul>
     * <li>Fail: If the properties could not be set or retrieved.
     * <li>Succeed: If all the properties can successfully be set and
     * retrieved.
     * </ul>
     * <li>Get/Set object filters
     * <ul>
     * <li>Fail: If a failure occurs during the set or get of an
     * ObjectFilter.
     * <li>Succeed: If an ObjectFilter can successfully be set and
     * retrieved.
     * </ul>
     * <li>Retrieve Rules from the RuleExecutionSet
     * <ul>
     * <li>Fail: If no rules are found.
     * <li>Succeed: If at least one rule definition can be found.
     * </ul>
     * </ul>
     */
    @Test
    void testRuleExecutionSet() {
        try {
            // Get the RuleServiceProvider
            RuleServiceProvider serviceProvider =
                    TestCaseUtil.getRuleServiceProvider("RuleExecutionSetTest");
            assert serviceProvider != null;

            // Get the RuleAdministrator
            RuleAdministrator ruleAdministrator = serviceProvider.getRuleAdministrator();
            assert ruleAdministrator != null;

            // Test the LocalRuleExecutionSetProvider API
            LocalRuleExecutionSetProvider localProvider = ruleAdministrator
                    .getLocalRuleExecutionSetProvider(null);
            assert localProvider != null;

            InputStream inStream = TestCaseUtil
                    .getRuleExecutionSetInputStream("src/test/resources/TckRes1.java");

            // Create an object filter.
            ObjectFilter filter = new TestObjectFilter(
                    TestObjectFilter.CUSTOMER_FILTER);

            // Create some properties.
            HashMap<Object, Object> props = new HashMap<>();
            props.put("org.evrete.jsr94.dsl-name", "JAVA-SOURCE");

            // Create the RuleExecutionSet with the properties.
            RuleExecutionSet res = localProvider.createRuleExecutionSet(
                    inStream, props);

            // set a property on the RuleExecutionSet
            res.setProperty("objectFilter", filter);
            res.setProperty("org.evrete.jsr94.ruleset-name", "Rule-set name");
            res.setProperty("org.evrete.jsr94.ruleset-description", "Rule-set description");

            inStream.close();

            // Test the basics.
            // Do we need to have a description ?
            assert res.getDescription() != null;
            assert res.getName() != null;

            // Test whether we can get the objectFilter back. The
            // object filter was specified as a property during
            // creation.
            Object obj = res.getProperty("objectFilter");
            assert obj != null;
            assert obj.equals(filter);

            // Test setting additional properties
            res.setProperty("additionalProperty", filter);
            obj = res.getProperty("additionalProperty");
            assert obj != null;
            assert filter.equals(obj);

            // Test setting a default object filter
            res.setDefaultObjectFilter(filter.getClass().getName());
            String objectFilter = res.getDefaultObjectFilter();
            assert objectFilter != null;
            assert objectFilter.equals(filter.getClass().getName());

            // Test the getRules.
            List<?> rules = res.getRules();
            assert rules != null;
            assert 0 < rules.size();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }
}
