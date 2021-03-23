package pkg1.evrete.tests.rule;

import org.evrete.api.RhsContext;
import org.evrete.dsl.DefaultSort;
import org.evrete.dsl.annotation.*;
import pkg1.evrete.tests.classes.*;

@RuleSortPolicy(DefaultSort.BY_NAME)
public class RuleSet2 {

    @Rule
    public void wrapIntegers(@Fact("$i") Integer i, RhsContext ctx) {
        // Replace integer with an instance of wrapper class inside the jar
        ctx.insert(new IntValue(i));
        // Delete initial fact
        ctx.deleteFact("$i");
    }

    @Rule
    @Where(asMethods = {@MethodPredicate(method = "test", descriptor = {"$i1.value", "$i2", "$i3.value"})})
    public void deleteNonPrime(@Fact("$i1") IntValue i1, @Fact("$i2") IntValue i2, @Fact("$i3") IntValue i3, RhsContext ctx) {
        ctx.deleteFact("$i3");
    }

    public static boolean test(int i1, IntValue i2, int i3) {
        return i3 == i1 * i2.value && !i2.testFile(i2);
        // The last term was always true, so it does not affect the logic
    }
}