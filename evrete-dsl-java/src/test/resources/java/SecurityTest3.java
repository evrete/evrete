package org.mypackage;

import org.evrete.dsl.TestUtils;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;

public class SecurityTest3 {

    @Rule
    public void rule1(@Fact("$i") Integer i) {
        TestUtils.testFile(i);
    }
}