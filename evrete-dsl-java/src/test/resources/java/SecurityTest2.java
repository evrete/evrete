package org.mypackage;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSet;
import org.evrete.dsl.annotation.Where;

@RuleSet(defaultSort = RuleSet.Sort.BY_NAME)
public class SecurityTest2 {

    @Rule
    @Where(value = "new File($i.toString()).exists()")
    public void rule1(@Fact("$i") Integer i) {
        System.out.println("Unreachable");
    }
}