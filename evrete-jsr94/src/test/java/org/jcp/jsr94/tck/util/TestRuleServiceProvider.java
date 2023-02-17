/*
 * JAVA COMMUNITY PROCESS
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck.util;

// java imports

import javax.rules.RuleServiceProvider;

/**
 * Utility class for the JSR-94 TCK.
 * <p>
 * This class extends the {@code javax.rules.RuleServiceProvider} and
 * is used to call the protected {@code createInstance} method.
 *
 * @version 1.0
 * @since JSR-94 1.0
 */
public abstract class TestRuleServiceProvider extends RuleServiceProvider {
    public static Object createInstance(String className)
            throws ClassNotFoundException,
            InstantiationException,
            IllegalAccessException {
        return RuleServiceProvider.createInstance(className);
    }
}
