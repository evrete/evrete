/*
 * J A V A  C O M M U N I T Y  P R O C E S S
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
 * This class extends the <code>javax.rules.RuleServiceProvider</code> and
 * is used to call the protected <code>createInstance</code> method.
 *
 * @version 1.0
 * @since JSR-94 1.0
 */
public abstract class TestRuleServiceProvider extends RuleServiceProvider {
    /**
     * @see javax.rules.RuleServiceProvider#createInstance
     */
    public static Object createInstance(String className)
            throws ClassNotFoundException,
            InstantiationException,
            IllegalAccessException {
        return RuleServiceProvider.createInstance(className);
    }
}
