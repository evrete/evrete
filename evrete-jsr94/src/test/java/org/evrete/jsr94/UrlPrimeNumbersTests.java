package org.evrete.jsr94;

import org.junit.jupiter.api.Test;

import javax.rules.*;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetCreateException;
import javax.rules.admin.RuleExecutionSetRegisterException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UrlPrimeNumbersTests {

    @Test
    void primeNumbers() throws ConfigurationException, ClassNotFoundException, IOException, RuleExecutionSetRegisterException, RuleExecutionSetCreateException, RuleSessionTypeUnsupportedException, RuleSessionCreateException, RuleExecutionSetNotFoundException, InvalidRuleSessionException {
        Class<?> implClass = Class.forName("org.evrete.jsr94.RuleServiceProviderImpl");
        String providerName = "prime numbers provider";

        RuleServiceProviderManager.registerRuleServiceProvider(providerName, implClass);
        RuleServiceProvider serviceProvider = RuleServiceProviderManager.getRuleServiceProvider(providerName);
        RuleAdministrator administrator = serviceProvider.getRuleAdministrator();


        // Ruleset configuration
        Map<Object, Object> config = new HashMap<>();
        config.put("org.evrete.jsr94.dsl-name", "JAVA-SOURCE");
        config.put("org.evrete.jsr94.ruleset-name", "Prime numbers ruleset");

        // Building the ruleset
        RuleExecutionSet ruleSet = administrator
                .getRuleExecutionSetProvider(null)
                .createRuleExecutionSet(
                        new URL("https://www.evrete.org/examples/PrimeNumbersSource.java"),
                        config
                );

        // Registering the ruleset
        String ruleSetName = ruleSet.getName();
        administrator.registerRuleExecutionSet(ruleSetName, ruleSet, null);

        // Get a RuleRuntime and invoke the rule engine.
        StatelessRuleSession session = (StatelessRuleSession) serviceProvider
                .getRuleRuntime()
                .createRuleSession(
                        ruleSetName,
                        new HashMap<>(),
                        RuleRuntime.STATELESS_SESSION_TYPE
                );

        // Create an input list.
        List<Object> input = new ArrayList<>();
        for (int i = 2; i < 100; i++) {
            input.add(i);
        }

        // Execute the rules
        List<?> results = session.executeRules(input);

        System.out.println("Result of calling executeRules: " + results);

        assert results.size() == 25;
        // Release the session.
        session.release();
        RuleServiceProviderManager.deregisterRuleServiceProvider(providerName);
    }

}
