package org.evrete.dsl.rules;

import org.evrete.api.RhsContext;
import org.evrete.api.events.SessionCreatedEvent;
import org.evrete.dsl.annotation.*;

public class DeclarationRuleSet4 {
    private int offset;

    @EventSubscription
    public void sessionStart(SessionCreatedEvent event) {
        this.offset = event.getSession().get("random-offset");
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
    public void rule(RhsContext ctx, @Fact("$i1") String $i1, @Fact("$i2") String i2, @Fact("$i3") String $i3) {
        ctx.delete($i3);
    }

    public boolean test(int i1, int i2, int i3) {
        return (i3 - offset) == (i1 - offset) * (i2 - offset);
    }
}
