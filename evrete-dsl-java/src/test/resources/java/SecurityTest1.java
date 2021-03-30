package org.mypackage;

import org.evrete.dsl.DefaultSort;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSortPolicy;

import java.io.File;
import java.nio.file.Paths;

@RuleSortPolicy(DefaultSort.BY_NAME)
public class SecurityTest1 {

    @Rule
    public void rule1(@Fact("$o") Object o) {
        new File(".").exists();
    }
}