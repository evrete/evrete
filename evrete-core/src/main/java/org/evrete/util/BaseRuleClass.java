package org.evrete.util;

import java.util.Objects;

/**
 * Base class for all generated Java sources. Developers can override this behavior
 * by setting the {@link org.evrete.Configuration#RULE_BASE_CLASS} property.
 * This class contains several sample static methods that can be used directly in literal conditions.
 * Use this class as a boilerplate for more advanced usage.
 */
public abstract class BaseRuleClass {

    @SuppressWarnings("unused")
    protected static boolean eq(Object o1, Object o2) {
        return Objects.equals(o1, o2);
    }

    @SuppressWarnings("unused")
    protected static boolean eq(Object... objects) {
        int len;
        switch (len = objects.length) {
            case 0:
            case 1:
                throw new IllegalArgumentException("Two or more arguments are expected");
            case 2:
                return eq(objects[0], objects[1]);
            default:
                Object first = objects[0];
                for (int i = 1; i < len; i++) {
                    if (!Objects.equals(first, objects[i])) {
                        return false;
                    }
                }
                return true;
        }
    }
}
