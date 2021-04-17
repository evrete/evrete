package org.mypackage;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSet;

import java.io.File;

@RuleSet(defaultSort = RuleSet.Sort.BY_NAME)
public class SecurityTest1 {

    @Rule
    public void rule1(@Fact("$o") Object o) {
        new File(".").exists();
    }
}