package org.evrete.dsl;

import java.util.List;

class DSLRule {
    final RuleMethod ruleMethod;
    final List<PredicateMethod> predicateMethods;

    DSLRule(RuleMethod ruleMethod, List<PredicateMethod> predicateMethods) {
        this.ruleMethod = ruleMethod;
        this.predicateMethods = predicateMethods;
    }
}
