package org.evrete.api;

import static org.evrete.api.FactBuilder.fact;

/**
 * <p>
 * A convenience interface to use for exists/notExists fact declarations.
 * </p>
 *
 * @param <T> the return type for fact selectors
 */
public interface ExistsFactSelector<T> {
    /**
     * <p>
     *     The main method which associates the implementation with a list fact type builders.
     * </p>
     *
     * @param facts an array of facts to declare
     * @return the generic type of the FactSelector
     */
    T having(FactBuilder... facts);

    default T having(String var, Class<?> type) {
        return having(
                fact(var, type)
        );
    }

    default T having(String var1, Class<?> type1, String var2, Class<?> type2) {
        return having(
                fact(var1, type1),
                fact(var2, type2)
        );
    }

    default T having(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3)
        );
    }

    default T having(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4)
        );
    }

    default T having(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5)
        );
    }

    default T having(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6)
        );
    }

    default T having(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6, String var7, Class<?> type7) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6),
                fact(var7, type7)
        );
    }

    default T having(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6, String var7, Class<?> type7, String var8, Class<?> type8) {
        return having(
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


    default T having(String var, String type) {
        return having(
                fact(var, type)
        );
    }

    default T having(String var1, String type1, String var2, String type2) {
        return having(
                fact(var1, type1),
                fact(var2, type2)
        );
    }

    default T having(String var1, String type1, String var2, String type2, String var3, String type3) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3)
        );
    }

    default T having(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4)
        );
    }

    default T having(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5)
        );
    }

    default T having(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5, String var6, String type6) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6)
        );
    }

    default T having(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5, String var6, String type6, String var7, String type7) {
        return having(
                fact(var1, type1),
                fact(var2, type2),
                fact(var3, type3),
                fact(var4, type4),
                fact(var5, type5),
                fact(var6, type6),
                fact(var7, type7)
        );
    }

    default T having(String var1, String type1, String var2, String type2, String var3, String type3, String var4, String type4, String var5, String type5, String var6, String type6, String var7, String type7, String var8, String type8) {
        return having(
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
