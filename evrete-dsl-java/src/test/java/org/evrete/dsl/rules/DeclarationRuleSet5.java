package org.evrete.dsl.rules;

import org.evrete.api.RhsContext;
import org.evrete.api.events.SessionFireEvent;
import org.evrete.dsl.annotation.*;

public class DeclarationRuleSet5 {

    @FieldDeclaration(name = "intField", type = "Hello world type")
    public static int intValue(String fact) {
        return Integer.parseInt(fact);
    }

    public static boolean test(int i1, int i2, int i3) {
        return i3 == i1 * i2;
    }

    @EventSubscription
    public void sessionStart(SessionFireEvent event) {
    }

    @Rule("Delete non-prime integers")
    @Where(methods = {@MethodPredicate(method = "test", args = {"$i1.intField", "$i2.intField", "$i3.intField"})})
    public void rule(RhsContext ctx, @Fact(value = "$i1", type = "Hello world type") String $i1, @Fact(value = "$i2", type = "Hello world type") String i2, @Fact(value = "$i3", type = "Hello world type") String $i3) {
        ctx.delete($i3);
    }
}
