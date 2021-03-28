package org.mypackage;

import org.evrete.dsl.DefaultSort;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSortPolicy;

@RuleSortPolicy(DefaultSort.BY_NAME_INVERSE)
public class RuleSet {

    @Rule(salience = -1)
    public void rule1(@Fact("$o") Object o) {

    }

    @Rule(value = "rule2", salience = 100)
    public void someName(@Fact("$o") Object o) {

    }

    @Rule(salience = 10)
    public void rule3(@Fact("$o") Object o) {

    }

    @Rule()
    public void rule4(@Fact("$o") Object o) {

    }

    @Rule()
    public void rule5(@Fact("$o") Object o) {

    }
}