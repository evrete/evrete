package org.mypackage;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

public class PrimeNumbers4 {

    @Rule("Delete non-prime integers")
    @Where(
            asMethods = {@MethodPredicate(method = "test", descriptor = {"$i1.intValue", "$i2.intValue", "$i3.intValue"})}
            )
    public void rule(RhsContext ctx, int $i1, @Fact("$i2") int i2, int $i3) {
        ctx.delete($i3);
    }

    public static boolean test(int i1, int i2, int i3) {
        return i3 == i1 * i2;
    }
}