package org.evrete.dsl.rules;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

public class SampleRuleSet2 {

    @SuppressWarnings({"unused"})
    public static boolean test(Integer i1, Integer i2, Integer i3) {
        return i3 == i1 * i2;
    }

    @SuppressWarnings({"unused", "MethodMayBeStatic"})
    @Rule("Delete non-prime integers")
    @Where(
            asMethods = {@MethodPredicate(method = "test", descriptor = {"$i1", "$i2", "$i3"})}
    )
    public void rule(RhsContext ctx, @Fact("$i1") int $i1, @Fact("$i2") int i2, @Fact("$i3") int $i3) {
        ctx.delete($i3);
    }

}
