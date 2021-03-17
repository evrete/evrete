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
import org.junit.jupiter.api.Test;

import javax.rules.*;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test the javax.rules.RuleRuntime class.
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testRuleRuntime}
 * <ul>
 * <li>Retrieve RuleRuntime
 * <li>Registration
 * <li>Create Stateless Rule session
 * <li>Create Stateful Rule session
 * </ul>
 * </ul>
 *
 * @version 1.0
 * @see javax.rules.RuleRuntime
 * @since JSR-94 1.0
 */
class RuleRuntimeTest {
    /**
     * Test the compliance for javax.rules.RuleRuntime.
     * This test will get the rule service provider and the
     * corresponding rule administrator. Both the stateless
     * (tck_res_1.xml) and stateful (tck_res_2.xml) rule execution sets
     * will be registered. The RuleRuntime is requested from the rule
     * service provider and the RuleRuntime interface API definition is
     * tested.
     * <p>
     * <b>Description:</b><br>
     * <ul>
     * <li>Retrieve RuleRuntime
     * <ul>
     * <li>Fail: If no RuleRuntime instance is returned by the rule
     * service provider.
     * <li>Succeed: If a RuleRuntime instance is returned by the rule
     * service provider.
     * </ul>
     * <li>Registration
     * <ul>
     * <li>Fail: If the registered stateless and stateful rule
     * execution sets are not part of the retrieved registration list.
     * <li>Succeed: If the registered rule execution sets are retrieved.
     * </ul>
     * <li>Create Stateless Rule session
     * <ul>
     * <li>Fail: If any error occurs when creating a stateless rule
     * session.
     * <li>Succeed: If a rule session can successfully be created and
     * is an instance of StatelessRuleSession.
     * </ul>
     * <li>Create Stateful Rule session
     * <ul>
     * <li>Fail: If any error occurs when creating a stateful rule
     * session.
     * <li>Succeed: If a rule session can successfully be created and
     * is an instance of StatefulRuleSession.
     * </ul>
     * </ul>
     *
     * @see TestCaseUtil#getRuleServiceProvider
     */
    @Test
    void testRuleRuntime() {
        try {
            String statelessUri = "src/test/resources/TckRes1.java";
            String statefulUri = "src/test/resources/TckRes2.java";


            // Get the RuleServiceProvider
            RuleServiceProvider serviceProvider =
                    TestCaseUtil.getRuleServiceProvider("ruleRuntimeTest");
            assert serviceProvider != null;

            // Get the RuleAdministrator
            RuleAdministrator ruleAdministrator =
                    serviceProvider.getRuleAdministrator();
            assert ruleAdministrator != null;

            // Get an input stream for the stateless test XML rule
            // execution  set.
            // Try to load the files from the "rule-execution-set-location".
            InputStream inStream = new FileInputStream(statelessUri);

            // parse the ruleset from source
            Map<String, String> config = new HashMap<>();
            config.put("org.evrete.jsr94.dsl-name", "JAVA-SOURCE");

            RuleExecutionSet res = ruleAdministrator
                    .getLocalRuleExecutionSetProvider(null)
                    .createRuleExecutionSet(inStream, config);

            assert res != null;
            inStream.close();

            // register the RuleExecutionSet
            ruleAdministrator.registerRuleExecutionSet(statelessUri,
                    res, null);

            // Get an input stream for the stateful test XML rule
            // execution  set.
            // Try to load the files from the "rule-execution-set-location".
            inStream = new FileInputStream(statefulUri);

            // parse the ruleset from the XML document
            res = ruleAdministrator.getLocalRuleExecutionSetProvider(null).
                    createRuleExecutionSet(inStream, config);
            assert res != null;
            inStream.close();

            // register the RuleExecutionSet
            ruleAdministrator.registerRuleExecutionSet(statefulUri,
                    res, null);

            // create a RuleRuntime
            RuleRuntime ruleRuntime = serviceProvider.getRuleRuntime();
            assert ruleRuntime != null;

            // So we finally have the RuleRuntime, now let's test it.
            List<?> registrations = ruleRuntime.getRegistrations();
            // Check that we retrieved registrations.
            assert registrations != null;

            // We should have at least two registrations. However other
            // test runs might have created registrations as well, so
            // test for at least 2.
            assert 2 <= registrations.size() : "Actual: " + registrations.size();

            // Check that we got the once that we registered.
            // i.e. statelessUri and statefulUri.
            Iterator<?> itr = registrations.iterator();

            int countRegistrations = 0;

            while (itr.hasNext()) {
                Object next = itr.next();
                assert next instanceof String;
                String name = (String) next;

                if (statelessUri.equals(name))
                    ++countRegistrations;

                if (statefulUri.equals(name))
                    ++countRegistrations;
            }
            // The count should be exactly 2 now, since we counted only
            // the once we registered.
            assert countRegistrations == 2;

            RuleSession ruleSession;

            // Create and check a stateless rule session.
            ruleSession = ruleRuntime.
                    createRuleSession(statelessUri, null,
                            RuleRuntime.STATELESS_SESSION_TYPE);
            assert ruleSession != null;
            assert ruleSession instanceof StatelessRuleSession;


            // Create and check a stateful rule session.
            ruleSession = ruleRuntime.createRuleSession(statefulUri, null,
                    RuleRuntime.STATEFUL_SESSION_TYPE);
            assert ruleSession != null;
            assert ruleSession instanceof StatefulRuleSession;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
