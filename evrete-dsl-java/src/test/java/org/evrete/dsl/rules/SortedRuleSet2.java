package org.evrete.dsl.rules;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSet;

@SuppressWarnings("unused")
@RuleSet(defaultSort = RuleSet.Sort.BY_NAME_INVERSE)
public class SortedRuleSet2 extends SortedRuleSetBase2 {

    @Rule(salience = 10)
    public void rule3(@Fact("$o") Object o) {

    }

    @Rule
    public void rule4(@Fact("$o") Object o) {

    }

    @Rule
    public void rule5(@Fact("$o") Object o) {

    }
}
