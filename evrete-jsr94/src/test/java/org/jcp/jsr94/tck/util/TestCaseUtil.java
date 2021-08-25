/*
 * J A V A  C O M M U N I T Y  P R O C E S S
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.rules.*;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for the JSR-94 TCK.
 * <p>
 * This class implements several convenience methods for accessing a
 * rule session.
 * It will parse the "tck.conf" configuration file. The "tck.conf"
 * configuration file is located in the lib directory of the tck
 * distribution.
 *
 * @version 1.0
 * @since JSR-94 1.0
 */
public abstract class TestCaseUtil {
    // The rule service provider as defined in the configuration file.
    private static String ruleServiceProvider;

    // The location of the rule execution set files.
    private static String ruleExecutionSetLocation;

    /**
     * Get the rule service provider configuration setting.
     * This method will parse the tck.conf configuration file and
     * return the definition of the rule-service-provider. This
     * definition should contain a valid class name for the rule engine
     * vendor specific implementation of the RuleServiceProvider.
     *
     * @return The class name of a RuleServiceProvider implementation.
     */
    public static String getRuleServiceProvider() {
        // The rule engine vendor specific rule service provider should
        // be specified in the tck.conf configuration file. The name
        // must be specified with the "rule-service-provider" tag.
        // Parse the configuration file, if we haven't done so.
        if (ruleServiceProvider == null)
            parseTckConfiguration();

        return ruleServiceProvider;
    }

    /**
     * This method complements the previous method. We suppose that the
     * rule provider implementation classes are stored in stand-alone JAR
     * files provided as URLs. The method creates a URLClassLoader, loads
     * the RuleServiceProvider class from the class loader, then registers
     * the object using the class loader.
     * <p>
     * Note: we need to take care about the CLASSPATH when launching
     * this test.
     *
     * @param uri  The registration name of this rule service provider.
     * @param urls The URLs which provide the rule service provoider classes.
     * @return The registered provider.
     */
    public static RuleServiceProvider getRuleServiceProvider(String uri,
                                                             URL[] urls)
            throws ClassNotFoundException, ConfigurationException {
        if (ruleServiceProvider == null) parseTckConfiguration();

        // Fail this test if no provider has been specified.
        if (ruleServiceProvider == null)
            throw new ClassNotFoundException("rule-service-provider not specified");

        ClassLoader cl = new URLClassLoader(urls);
        Class<?> ruleServiceProviderClass = cl.loadClass(ruleServiceProvider);
/*
        TestCase.assertNotNull("[TestCaseUtil] Class loading on " +
                        ruleServiceProvider + " failed. ",
                ruleServiceProviderClass);
*/
        assert ruleServiceProviderClass != null;

        // Register the provider.
        RuleServiceProviderManager.registerRuleServiceProvider(uri,
                ruleServiceProviderClass, cl);
        // Retrieve and return the provider
        return RuleServiceProviderManager.getRuleServiceProvider(uri);
    }

    /**
     * Get a RuleServiceProvider.
     * <p>
     * Get a rule engine vendor specific rule service provider. The
     * provider is constucted in the following manner:<br>
     * <ul>
     * <li>Get the "rule-service-provider" from the configuration file.
     * <li>Load the RuleServiceProvider class specified by this property.
     * <li>Register this RuleServiceProvider class.
     * </ul>
     *
     * @param uri The registration name of this rule service provider.
     * @return The registered provider.
     */
    public static RuleServiceProvider getRuleServiceProvider(String uri)
            throws ClassNotFoundException, ConfigurationException {
        // The rule engine vendor specific rule service provider should
        // be specified in the tck.conf configuration file. The name
        // must be specified with the "rule-service-provider" tag.
        // Parse the configuration file, if we haven't done so.
        if (ruleServiceProvider == null)
            parseTckConfiguration();

        // Fail this test if no provider has been specified.
        if (ruleServiceProvider == null) {
            throw new ClassNotFoundException("rule-service-provider not specified");
        }

        // Load the provider.
        Class<?> ruleServiceProviderClass = Class.forName(ruleServiceProvider);

        // Register the provider.
        RuleServiceProviderManager.registerRuleServiceProvider(uri,
                ruleServiceProviderClass);
        // Retrieve and return the provider
        return RuleServiceProviderManager.getRuleServiceProvider(uri);
    }


    /**
     * Get a StatefulRuleSession.
     * <p>
     * Get a stateful rule session. The provider is constucted in the
     * following manner:<br>
     * <ul>
     * <li>Get a RuleServiceProvider {@link #getRuleServiceProvider}.
     * <li>Get the RuleAdministrator and create/register a rule
     * execution set.
     * <li>Get the RuleRuntime and create a rule session on the
     * registered rule execution set.
     * </ul>
     * <p>
     * <b>Note:</b><br>
     * The location of the rule execution sets is specified in the
     * tck.conf configuration file with the tag
     * "rule-execution-set-location".
     *
     * @param uri                 The registration name of this rule service provider.
     * @param ruleExecutionSetUri The file name of the rule execution set
     * @return The stateful rule session.
     */
    public static StatefulRuleSession getStatefulRuleSession(String uri, String ruleExecutionSetUri)
            throws Exception {
        // Get the RuleServiceProvider
        RuleServiceProvider serviceProvider = getRuleServiceProvider(uri);
        assert serviceProvider != null;

        // Get the RuleAdministrator
        RuleAdministrator ruleAdministration =
                serviceProvider.getRuleAdministrator();
        assert ruleAdministration != null;

        // Get an input stream to a test XML rule execution set.
        // Try to load the files from the "rule-execution-set-location".
        InputStream inStream = getRuleExecutionSetInputStream(ruleExecutionSetUri);

        assert inStream != null;

        Map<String, String> resourceConfig = new HashMap<>();
        String dsl;
        if (ruleExecutionSetUri.endsWith(".java")) {
            dsl = "JAVA-SOURCE";
        } else if (ruleExecutionSetUri.endsWith(".class")) {
            dsl = "JAVA_CLASS";
        } else if (ruleExecutionSetUri.endsWith(".jar")) {
            dsl = "JAVA_JAR";
        } else {
            throw new IllegalArgumentException("Unknown resource extension for " + ruleExecutionSetUri);
        }
        resourceConfig.put("org.evrete.jsr94.dsl-name", dsl);

        // parse the ruleset from the XML document
        RuleExecutionSet res = ruleAdministration.
                getLocalRuleExecutionSetProvider(null).
                createRuleExecutionSet(inStream, resourceConfig);

        assert res != null;

        inStream.close();

        // register the RuleExecutionSet
        ruleAdministration.registerRuleExecutionSet(uri, res, null);

        // create a stateless RuleSession
        RuleRuntime ruleRuntime = serviceProvider.getRuleRuntime();
/*
        TestCase.assertNotNull("[TestCaseUtil] RuleRuntime not created.",
                ruleRuntime);
*/
        assert ruleRuntime != null;

        StatefulRuleSession session = (StatefulRuleSession)
                ruleRuntime.createRuleSession(uri,
                        null,
                        RuleRuntime.STATEFUL_SESSION_TYPE);

/*
        TestCase.assertNotNull("[TestCaseUtil] StatefulRuleSession not created.",
                session);
*/
        assert session != null;

        return session;
    }

    /**
     * Get a StatelessRuleSession.
     * <p>
     * Get a stateless rule session. The provider is constucted in the
     * following manner:<br>
     * <ul>
     * <li>Get a RuleServiceProvider {@link #getRuleServiceProvider}.
     * <li>Get the RuleAdministrator and create/register a rule
     * execution set.
     * <li>Get the RuleRuntime and create a rule session on the
     * registered rule execution set.
     * </ul>
     * <p>
     * <b>Note:</b><br>
     * The location of the rule execution sets is specified in the
     * tck.conf configuration file with the tag
     * "rule-execution-set-location".
     *
     * @param uri                 The registration name of this rule service provider.
     * @param ruleExecutionSetUri The file name of the rule execution set
     * @return The stateless rule session.
     */
    public static StatelessRuleSession getStatelessRuleSession(String uri,
                                                               String ruleExecutionSetUri)
            throws Exception {
        // Get the RuleServiceProvider
        RuleServiceProvider serviceProvider = getRuleServiceProvider(uri);
/*
        TestCase.assertNotNull("[TestCaseUtil] RuleServiceProvider " +
                uri + " not found.", serviceProvider);
*/

        assert serviceProvider != null;

        // Get the RuleAdministrator
        RuleAdministrator ruleAdministration = serviceProvider.getRuleAdministrator();
/*
        TestCase.assertNotNull("[TestCaseUtil] RuleAdministrator not found.",
                ruleAdministration);
*/

        assert ruleAdministration != null;

        // Get an input stream to a test XML rule execution set.
        // Try to load the files from the "rule-execution-set-location".
        InputStream inStream = getRuleExecutionSetInputStream(ruleExecutionSetUri);

        Map<String, String> resourceConfig = new HashMap<>();
        String dsl;
        if (ruleExecutionSetUri.endsWith(".java")) {
            dsl = "JAVA-SOURCE";
        } else if (ruleExecutionSetUri.endsWith(".class")) {
            dsl = "JAVA_CLASS";
        } else if (ruleExecutionSetUri.endsWith(".jar")) {
            dsl = "JAVA_JAR";
        } else {
            throw new IllegalArgumentException("Unknown resource extension for " + ruleExecutionSetUri);
        }

        resourceConfig.put("org.evrete.jsr94.dsl-name", dsl);
        // parse the ruleset from the XML document
        RuleExecutionSet res = ruleAdministration.getLocalRuleExecutionSetProvider(null).createRuleExecutionSet(inStream, resourceConfig);
/*
        TestCase.assertNotNull("[TestCaseUtil] RuleExecutionSet " +
                        ruleExecutionSetUri +
                        " could not be created.",
                res);
*/

        assert res != null;
        inStream.close();

        // register the RuleExecutionSet
        ruleAdministration.registerRuleExecutionSet(uri, res, null);

        // create a stateless RuleSession
        RuleRuntime ruleRuntime = serviceProvider.getRuleRuntime();
/*
        TestCase.assertNotNull("[TestCaseUtil] RuleRuntime not created.",
                ruleRuntime);
*/

        assert ruleRuntime != null;

        return (StatelessRuleSession) ruleRuntime.createRuleSession(uri,
                null, RuleRuntime.STATELESS_SESSION_TYPE);

    }

    /**
     * Get a rule execution set input stream.
     * This method will return an input stream to the RuleExecutionSet
     * for  the specified uri.
     * <b>Note:</b><br>
     * The location of the rule execution sets is specified in the
     * tck.conf configuration file with the tag
     * "rule-execution-set-location".
     *
     * @param ruleExecutionSetUri The file name of the rule execution
     *                            set.
     * @return The InputStream to the specified rule execution set.
     */
    public static InputStream getRuleExecutionSetInputStream(String ruleExecutionSetUri) {
        InputStream inStream = null;

        try {
            // Get an input stream to a test XML rule execution set.
            // Try to load the files from the "rule-execution-set-location".
            inStream = new FileInputStream(ruleExecutionSetUri);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
        return inStream;
    }


    /**
     * Get a rule execution set reader.
     * This method will return a Reader to the RuleExecutionSet
     * for  the specified uri.
     * <b>Note:</b><br>
     * The location of the rule execution sets is specified in the
     * tck.conf configuration file with the tag
     * "rule-execution-set-location".
     *
     * @param ruleExecutionSetUri The file name of the rule execution
     *                            set.
     * @return The Reader to the specified rule execution set.
     */
    public static Reader getRuleExecutionSetReader(
            String ruleExecutionSetUri) {
        // The rule execution set location is to be specified in the
        // tck.conf configuration file. The name
        // must be specified with the "rule-execution-set-location" tag.
        // Parse the configuration file, if we haven't done so.
        if (ruleExecutionSetLocation == null)
            parseTckConfiguration();

        Reader reader;

        try {
            // Get an input stream to a test XML rule execution set.
            // Try to load the files from the "rule-execution-set-location".
            reader = new FileReader(ruleExecutionSetUri);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
        return reader;
    }


    /**
     * Parse the TCK configuration file.
     * This file should contain settings for the following:
     * ruleServiceProvider: The rule engine vendor specific rule
     * service provider implementation.
     * ruleExecutionSetLocation: The location where the TCK test rule
     * execution sets can be found.
     */
    private static void parseTckConfiguration() {
        try {
            // Is there a system property defined for the location of
            // the configuration file?
            InputStream inStream = new FileInputStream("src/test/resources/tck.xml");
            Document configurationDoc = null;

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            configurationDoc = db.parse(inStream);
            inStream.close();

            Element documentElement = configurationDoc.getDocumentElement();
            NodeList confNodeList = documentElement.getChildNodes();

            for (int i = 0; i < confNodeList.getLength(); i++) {
                Node childNode = confNodeList.item(i);
                if (childNode != null) {
                    String nodeName = childNode.getNodeName();

                    switch (nodeName) {
                        case "rule-service-provider":
                            ruleServiceProvider = childNode.getFirstChild().getNodeValue();
                            break;
                        case "rule-service-provider-jar-url":
                            break;
                        case "rule-execution-set-location":
                            ruleExecutionSetLocation = childNode.getFirstChild().getNodeValue();
                            break;
                    }
                }
            }
        } catch (Exception ex) {
            // Print the stack trace.
            ex.printStackTrace();
        }
    }

    /**
     * Processes an unexpected exception thrown in a JUnit test method.
     * This message extracts (if possible) the line number from the exception
     * and displays appropriate information for easy debugging. Then
     * it fails the test by calling <code>fail</code>.
     *
     * @param e        The target exception to be processed.
     * @param test     The test case.
     * @param methName The method name in which the exception is caught.
     */
/*
    public static void processTestException(Exception e,
                                            TestCase test,
                                            String methName) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        e.printStackTrace(printer);
        printer.flush();
        String msg = writer.toString();
        String lineno = null;

        String fullname = test.getClass().getName() + "." + methName;
        StringTokenizer tokens = new StringTokenizer(msg, "\n\r");
        while (tokens.hasMoreElements()) {
            String next = (String) tokens.nextElement();
            if (next.indexOf(fullname) >= 0) {
                // return next;
                int i1 = next.lastIndexOf(':');
                if (i1 >= 0) {
                    int i2 = next.lastIndexOf(')');
                    if ((i2 >= 0) && (i2 > i1)) {
                        lineno = next.substring(i1 + 1, i2);
                        break;
                    }
                }
            }
        }

        if (lineno != null)
            Assert.fail("Exception thrown at line " + lineno + ":\n" + e.getMessage());
        else
            Assert.fail("Exception thrown:\n" + e.getMessage());
    }
*/
}
