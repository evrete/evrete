package org.mypackage;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;
import org.evrete.dsl.issues.model.DayTrendFact;

public class ImportTest1 {
    private static final int PRIORITY_A = 100;

    @Rule(salience = PRIORITY_A)
    @Where("$x.value > 0")
    public void initMACD(DayTrendFact $x, RhsContext ctx) {
        $x.value = -1;
    }
}