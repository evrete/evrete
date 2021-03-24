package org.evrete.samples;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

/**
 * <p>
 * Class sample source used in {@link PrimeNumbersDSLSource}
 * </p>
 */
@SuppressWarnings("unused")
public class PrimeNumbersSource {
    @Rule
    @Where("$i1 * $i2 == $i3")
    public static void rule(RhsContext ctx, int $i1, int $i2, int $i3) {
        ctx.deleteFact("$i3");
    }
}
