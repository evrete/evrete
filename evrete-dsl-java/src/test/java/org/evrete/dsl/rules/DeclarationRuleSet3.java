package org.evrete.dsl.rules;

import org.evrete.api.Environment;
import org.evrete.api.RhsContext;
import org.evrete.dsl.Phase;
import org.evrete.dsl.annotation.*;

public class DeclarationRuleSet3 {

    @FieldDeclaration()
    public static int intValue(String fact) {
        return Integer.parseInt(fact);
    }

    public static boolean test(int i1, int i2, int i3) {
        return i3 == i1 * i2;
    }

    @PhaseListener(Phase.FIRE)
    public void sessionStart(Environment environment) {
    }

    @Rule("Delete non-prime integers")
    @Where(asMethods = {@MethodPredicate(method = "test", descriptor = {"$i1.intValue", "$i2.intValue", "$i3.intValue"})})
    public void rule(RhsContext ctx, @Fact("$i1") String $i1, @Fact("$i2") String i2, @Fact("$i3") String $i3) {
        ctx.delete($i3);
    }
}
