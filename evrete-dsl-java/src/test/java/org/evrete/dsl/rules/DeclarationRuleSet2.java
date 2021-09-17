package org.evrete.dsl.rules;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.FieldDeclaration;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

public class DeclarationRuleSet2 {

    /*
     * declares an int field named "intValue" on String fact types
     */
    @FieldDeclaration()
    public int intValue(String fact) {
        return Integer.parseInt(fact);
    }


    @Rule("Delete non-prime integers")
    @Where(value = {"$i3.intValue == $i1.intValue * $i2.intValue"})
    public void rule(RhsContext ctx, @Fact("$i1") String $i1, @Fact("$i2") String i2, @Fact("$i3") String $i3) {
        ctx.delete($i3);
    }
}
