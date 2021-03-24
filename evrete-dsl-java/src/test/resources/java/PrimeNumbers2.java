package org.mypackage;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

public class PrimeNumbers2 {

    @Rule("Delete non-prime integers")
    @Where(value = {"$i3 == $i1 * $i2"})
    public static void rule(RhsContext ctx, int $i1, @Fact("$i2") int i2, int $i3) {
        ctx.delete($i3);
    }
}