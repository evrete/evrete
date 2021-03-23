package pkg1.evrete.tests.rule;

import org.evrete.api.RhsContext;
import org.evrete.dsl.DefaultSort;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSortPolicy;
import org.evrete.dsl.annotation.Where;
import pkg1.evrete.tests.classes.*;

@RuleSortPolicy(DefaultSort.BY_NAME)
public class RuleSet1 {

    @Rule
    public void wrapIntegers(@Fact("$i") Integer i, RhsContext ctx) {
        // Replace integer with an instance of wrapper class inside the jar
        ctx.insert(new IntValue(i));
        // Delete initial fact
        ctx.deleteFact("$i");
    }

    @Rule
    @Where(asStrings = {"$i3.value == $i1.value * $i2.value"})
    public void deleteNonPrime(@Fact("$i1") IntValue i1, @Fact("$i2") IntValue i2, @Fact("$i3") IntValue i3, RhsContext ctx) {
        ctx.deleteFact("$i3");
    }
}