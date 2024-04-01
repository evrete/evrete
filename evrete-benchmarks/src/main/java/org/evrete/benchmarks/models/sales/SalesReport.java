package org.evrete.benchmarks.models.sales;

import org.evrete.benchmarks.RuleEngines;

import java.util.HashMap;
import java.util.Map;

public class SalesReport {
    private final Map<SalesUnit, Double> sales = new HashMap<>();

    public void add(SalesUnit unit, double amount) {
        Double total = sales.get(unit);
        if (total == null) {
            total = amount;
        } else {
            total += amount;
        }
        sales.put(unit, total);
        RuleEngines.rhsLoad();
    }

    @Override
    public String toString() {
        return "SalesReport{" +
                "sales=" + sales +
                '}';
    }
}
