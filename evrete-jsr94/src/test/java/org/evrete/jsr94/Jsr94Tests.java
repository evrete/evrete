package org.evrete.jsr94;

import org.evrete.jsr94.classes.Example1;
import org.jcp.jsr94.tck.model.Customer;
import org.jcp.jsr94.tck.model.Invoice;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.rules.*;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetCreateException;
import javax.rules.admin.RuleExecutionSetRegisterException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.evrete.jsr94.RuleServiceProviderImpl.RULE_SERVICE_PROVIDER;

class Jsr94Tests {
    private static RuleServiceProvider serviceProvider;

    @BeforeAll
    static void setUpClass() {
        try {
            Class<?> clazz = Class.forName(RuleServiceProviderImpl.class.getName());
            RuleServiceProviderManager.registerRuleServiceProvider(RULE_SERVICE_PROVIDER, clazz);
            serviceProvider = RuleServiceProviderManager.getRuleServiceProvider(RULE_SERVICE_PROVIDER);

        } catch (ClassNotFoundException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    static void shutDownClass() {
        RuleServiceProviderManager.deregisterRuleServiceProvider(org.evrete.jsr94.RuleServiceProviderImpl.RULE_SERVICE_PROVIDER);
    }

    private static InputStream classBytesStream(Class<?> ruleClass) {
        String url = ruleClass.getName().replaceAll("\\.", "/") + ".class";
        return ruleClass.getClassLoader().getResourceAsStream(url);
    }

    @Test
    void testInit1() throws ConfigurationException, IOException, RuleExecutionSetRegisterException, RuleExecutionSetCreateException, RuleSessionTypeUnsupportedException, RuleSessionCreateException, RuleExecutionSetNotFoundException, InvalidRuleSessionException {

        // get the RuleAdministrator
        RuleAdministrator administrator = serviceProvider.getRuleAdministrator();

        // get an input stream to a test XML ruleset
        // This rule execution set is part of the TCK.
        InputStream inStream = classBytesStream(Example1.class);

        // parse the ruleset from the XML document
        Map<Object, Object> ruleSetConfig = new HashMap<>();
        ruleSetConfig.put(Const.DSL_NAME, "JAVA-CLASS");
        ruleSetConfig.put(Const.RULE_SET_NAME, "JSR-94 Example");
        ruleSetConfig.put(Const.RULE_SET_DESCRIPTION, "A simple rule set that removes all non-prime numbers from working memory");
        RuleExecutionSet res1 = administrator
                .getLocalRuleExecutionSetProvider(null)
                .createRuleExecutionSet(inStream, ruleSetConfig);
        inStream.close();

        // register the RuleExecutionSet
        String uri = res1.getName();
        administrator.registerRuleExecutionSet(uri, res1, null);

        // Get a RuleRuntime and invoke the rule engine.
        RuleRuntime ruleRuntime = serviceProvider.getRuleRuntime();

        // create a StatelessRuleSession
        StatelessRuleSession
                statelessRuleSession =
                (StatelessRuleSession) ruleRuntime.createRuleSession(uri,
                        new HashMap<>(), RuleRuntime.STATELESS_SESSION_TYPE);

        System.out.println("Got Stateless Rule Session: " +
                statelessRuleSession);

        // call executeRules with some input objects

        // Create a Customer as specified by the TCK documentation.
        Customer inputCustomer = new Customer("test");
        inputCustomer.setCreditLimit(5000);

        // Create an Invoice as specified by the TCK documentation.
        Invoice inputInvoice = new Invoice("Invoice 1");
        inputInvoice.setAmount(2000);

        // Create a input list.
        List<Object> input = new ArrayList<>();
        input.add(inputCustomer);
        input.add(inputInvoice);

        // Print the input.
        System.out.println("Calling rule session with the following data");
        System.out.println("Customer credit limit input: " +
                inputCustomer.getCreditLimit());
        System.out.println(inputInvoice.getDescription() +
                " amount: " + inputInvoice.getAmount() +
                " status: " + inputInvoice.getStatus());

        // Execute the rules without a filter.
        List<?> results = statelessRuleSession.executeRules(input);

        System.out.println("Called executeRules on Stateless Rule Session: " + statelessRuleSession);

        System.out.println("Result of calling executeRules: " +
                results.size() + " results.");

        // Loop over the results.
        Iterator<?> itr = results.iterator();
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof Customer)
                System.out.println("Customer credit limit result: " +
                        ((Customer) obj).getCreditLimit());
            if (obj instanceof Invoice)
                System.out.println(((Invoice) obj).getDescription() +
                        " amount: " + ((Invoice) obj).getAmount() +
                        " status: " + ((Invoice) obj).getStatus());
        }

        // Release the session.
        statelessRuleSession.release();
        System.out.println("Released Stateless Rule Session.");
        System.out.println();

        // create a StatefulRuleSession
        StatefulRuleSession statefulRuleSession =
                (StatefulRuleSession) ruleRuntime.createRuleSession(uri,
                        new HashMap<>(),
                        RuleRuntime.STATEFUL_SESSION_TYPE);

        System.out.println("Got Stateful Rule Session: " + statefulRuleSession);
        // Add another Invoice.
        Invoice inputInvoice2 = new Invoice("Invoice 2");
        inputInvoice2.setAmount(1750);
        input.add(inputInvoice2);
        System.out.println("Calling rule session with the following data");
        System.out.println("Customer credit limit input: " +
                inputCustomer.getCreditLimit());
        System.out.println(inputInvoice.getDescription() +
                " amount: " + inputInvoice.getAmount() +
                " status: " + inputInvoice.getStatus());
        System.out.println(inputInvoice2.getDescription() +
                " amount: " + inputInvoice2.getAmount() +
                " status: " + inputInvoice2.getStatus());

        // add an Object to the statefulRuleSession
        statefulRuleSession.addObjects(input);
        System.out.println("Called addObject on Stateful Rule Session: "
                + statefulRuleSession);

        statefulRuleSession.executeRules();
        System.out.println("Called executeRules");

        // extract the Objects from the statefulRuleSession
        results = statefulRuleSession.getObjects();

        System.out.println("Result of calling getObjects: " +
                results.size() + " results.");


        // Loop over the results.
        itr = results.iterator();
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof Customer)
                System.out.println("Customer credit limit result: " +
                        ((Customer) obj).getCreditLimit());
            if (obj instanceof Invoice)
                System.out.println(((Invoice) obj).getDescription() +
                        " amount: " + ((Invoice) obj).getAmount() +
                        " status: " + ((Invoice) obj).getStatus());
        }

        // release the statefulRuleSession
        statefulRuleSession.release();
        System.out.println("Released Stateful Rule Session.");
        System.out.println();

    }

}
