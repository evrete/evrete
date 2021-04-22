package org.evrete.dsl.rules;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;

@SuppressWarnings("unused")
public class SortedRuleSet1 extends SortedRuleSetBase1 {
    @Rule(salience = 1)
    public void rule1(@Fact("$o") Object o) {

    }

    @Rule(value = "rule2", salience = 100)
    public void someName(@Fact("$o") Object o) {

    }

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
