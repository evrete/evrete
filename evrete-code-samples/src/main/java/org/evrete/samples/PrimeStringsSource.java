package org.evrete.samples;

import org.evrete.api.Environment;
import org.evrete.api.RhsContext;
import org.evrete.dsl.Phase;
import org.evrete.dsl.annotation.*;

@RuleSet
public class PrimeStringsSource {
    private int offset;

    @PhaseListener(Phase.FIRE)
    public void sessionStart(Environment environment) {
        this.offset = environment.get("random-offset");
    }

    @FieldDeclaration()
    public int intValue(String fact) {
        return Integer.parseInt(fact) + offset;
    }

    @Rule("Delete non-prime integers")
    @Where(
            methods = @MethodPredicate(
                    method = "test",
                    args = {"$i1.intValue", "$i2.intValue", "$i3.intValue"}
            )
    )
    public void rule(RhsContext ctx, String $i1, String $i2, String $i3) {
        ctx.delete($i3);
    }

    @SuppressWarnings("unused")
    public boolean test(int i1, int i2, int i3) {
        return (i3 - offset) == (i1 - offset) * (i2 - offset);
    }
}
