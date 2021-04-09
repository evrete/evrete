package org.evrete.benchmarks.models.sales;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    }

    @Override
    public String toString() {
        return "SalesReport{" +
                "sales=" + sales +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SalesReport that = (SalesReport) o;
        return Objects.equals(sales, that.sales);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sales);
    }
}
