package org.evrete.jsr94;

import javax.rules.RuleRuntime;
import javax.rules.RuleServiceProvider;
import javax.rules.RuleServiceProviderManager;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import java.io.InputStream;

import static org.evrete.jsr94.RuleServiceProviderImpl.RULE_SERVICE_PROVIDER;

public class Main {

    public static void main(String[] args) {
        try {
            doStuff();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void doStuff() throws Exception {
// Load the rule service provider of the reference
        // implementation.
        // Loading this class will automatically register this
        // provider with the provider manager.
        Class.forName(RuleServiceProviderImpl.class.getName());

        // Get the rule service provider from the provider manager.
        RuleServiceProvider serviceProvider = RuleServiceProviderManager.getRuleServiceProvider(RULE_SERVICE_PROVIDER);


        // get the RuleAdministrator
        RuleAdministrator ruleAdministrator = serviceProvider.getRuleAdministrator();
        System.out.println("\nAdministration API\n");
        System.out.println("Acquired RuleAdministrator: " +
                ruleAdministrator);

        // get an input stream to a test XML ruleset
        // This rule execution set is part of the TCK.
        InputStream inStream = Main.class.getResourceAsStream("/org/jcp/jsr94/tck/tck_res_1.xml");
        System.out.println("Acquired InputStream to RI tck_res_1.xml: " +
                inStream);

        // parse the ruleset from the XML document
        RuleExecutionSet res1 = ruleAdministrator.getLocalRuleExecutionSetProvider(null).createRuleExecutionSet(inStream, null);
        inStream.close();
        System.out.println("Loaded RuleExecutionSet: " + res1);

        // register the RuleExecutionSet
        String uri = res1.getName();
        ruleAdministrator.registerRuleExecutionSet(uri, res1, null);
        System.out.println("Bound RuleExecutionSet to URI: " + uri);


        // Get a RuleRuntime and invoke the rule engine.
        System.out.println("\nRuntime API\n");

        RuleRuntime ruleRuntime = serviceProvider.getRuleRuntime();
        System.out.println("Acquired RuleRuntime: " + ruleRuntime);

    }
}
