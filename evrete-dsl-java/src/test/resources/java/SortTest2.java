package org.mypackage;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;

public class RuleSet {

    @Rule
    public void rule1(@Fact("$o") Object o) {

    }

    @Rule("rule2")
    public void someName(@Fact("$o") Object o) {

    }

    @Rule
    public void rule3(@Fact("$o") Object o) {

    }
}