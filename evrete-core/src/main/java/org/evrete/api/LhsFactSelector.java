package org.evrete.api;

import java.util.Arrays;
import java.util.Collection;

import static org.evrete.api.FactBuilder.fact;

/**
 * <p>
 * A convenience interface to use for LHS fact declarations.
 * </p>
 *
 * @param <T> the return type for fact selectors
 */
@SuppressWarnings("ALL")
public interface LhsFactSelector<T> {
    /**
     * <p>
     * The main method which associates the implementation with a list fact type builders.
     * </p>
     *
     * @param facts a collection of facts to declare
     * @return the generic type of the FactSelector
     */
    T forEach(Collection<FactBuilder> facts);

    default T forEach(FactBuilder... facts) {
        return forEach(Arrays.asList(facts));
    }

    default T forEach(String var, Class<?> type) {
        return forEach(
                fact(var, type)
        );
    }

    default T forEach(String var1, Class<?> type1, String var2, Class<?> type2) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2)
        );
    }

    default T forEach(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3)
        );
    }

    default T forEach(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4)
        );
    }

    default T forEach(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5)
        );
    }

    default T forEach(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6)
        );
    }

    default T forEach(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6, String var7, Class<?> type7) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6),
                fact(var7, type7)
        );
    }

    default T forEach(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6, String var7, Class<?> type7, String var8, Class<?> type8) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6),
                fact(var7, type7),
                fact(var8, type8)
        );
    }


    default T forEach(String var, String type) {
        return forEach(
                fact(var, type)
        );
    }

    default T forEach(String var1, String type1, String var2, String type2) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2)
        );
    }

    default T forEach(String var1, String type1, String var2, String type2, String var3, String type3) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3)
        );
    }

    default T forEach(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4)
        );
    }

    default T forEach(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5)
        );
    }

    default T forEach(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5, String var6, String type6) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6)
        );
    }

    default T forEach(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5, String var6, String type6, String var7, String type7) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6),
                fact(var7, type7)
        );
    }

    default T forEach(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5, String var6, String type6, String var7, String type7, String var8, String type8) {
        return forEach(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6),
                fact(var7, type7),
                fact(var8, type8)
        );
    }
}
