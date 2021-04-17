package org.evrete.dsl.rules;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSet;

@RuleSet(defaultSort = RuleSet.Sort.BY_NAME_INVERSE)
@SuppressWarnings("unused")
public class SortedRuleSetBase1 {
    private static final int SALIENCE_THAT_WILL_BE_OVERRIDDEN = 777;

    @Rule(value = "will be overridden", salience = SALIENCE_THAT_WILL_BE_OVERRIDDEN)
    public void rule1(@Fact("$o") Object o) {

    }

    @Rule(value = "will be overridden", salience = SALIENCE_THAT_WILL_BE_OVERRIDDEN)
    public void someName(@Fact("$o") Object o) {

    }

    @Rule(value = "will be overridden", salience = SALIENCE_THAT_WILL_BE_OVERRIDDEN)
    public void rule3(@Fact("$o") Object o) {

    }

    @Rule(value = "will be overridden", salience = SALIENCE_THAT_WILL_BE_OVERRIDDEN)
    public void rule4(@Fact("$o") Object o) {

    }

    @Rule()
    public void rule5(@Fact("$o") Object o) {

    }
}
