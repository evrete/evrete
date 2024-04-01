package org.evrete.benchmarks.models.sales;

public class SalesUnit {
    public final int id;

    public SalesUnit(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SalesUnit{" +
                "id=" + id +
                '}';
    }
}
