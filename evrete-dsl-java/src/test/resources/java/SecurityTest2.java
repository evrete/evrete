package org.mypackage;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSortPolicy;
import org.evrete.dsl.DefaultSort;
import org.evrete.dsl.annotation.Where;

@RuleSortPolicy(DefaultSort.BY_NAME)
public class SecurityTest2 {

    @Rule
    @Where(asStrings = "new File($i.toString()).exists()")
    public void rule1(@Fact("$i") Integer i) {
        System.out.println("Unreachable");
    }
}