package org.evrete.dsl.rules;

import org.evrete.api.Environment;
import org.evrete.api.RhsContext;
import org.evrete.dsl.Phase;
import org.evrete.dsl.annotation.*;

public class DeclarationRuleSet2 {

    /*
     * declares an int field named "intValue" on String fact types
     */
    @FieldDeclaration()
    public static int intValue(String fact) {
        return Integer.parseInt(fact);
    }

    @PhaseListener(Phase.FIRE)
    public static void init(Environment environment) {

    }

    @Rule("Delete non-prime integers")
    @Where(value = {"$i3.intValue == $i1.intValue * $i2.intValue"})
    @SuppressWarnings({"MethodMayBeStatic"})
    public void rule(RhsContext ctx, @Fact("$i1") String $i1, @Fact("$i2") String i2, @Fact("$i3") String $i3) {
        ctx.delete($i3);
    }
}
