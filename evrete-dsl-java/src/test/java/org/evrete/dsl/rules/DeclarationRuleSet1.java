package org.evrete.dsl.rules;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.*;

@RuleSet
public class DeclarationRuleSet1 {

    /*
     * declares an int field named "intValue" on String fact types
     */
    @FieldDeclaration(name = "intValue")
    public static int toNumber(String fact) {
        return Integer.parseInt(fact);
    }

    @Rule("Delete non-prime integers")
    @Where(value = {"$i3.intValue == $i1.intValue * $i2.intValue"})
    public void rule(RhsContext ctx, @Fact("$i1") String $i1, @Fact("$i2") String i2, @Fact("$i3") String $i3) {
        ctx.delete($i3);
    }

}
