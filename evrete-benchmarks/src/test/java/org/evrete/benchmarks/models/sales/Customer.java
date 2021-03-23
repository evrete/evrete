package org.evrete.benchmarks.models.sales;

public class Customer {
    public final int id;
    public final String name;

    public Customer(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Customer(int id) {
        this(id, "customer" + id);
    }

    public boolean sameName(String name) {
        waitNs(1000);
        return name.equals(this.name);
    }

    private void waitNs(long i) {
        long start = System.nanoTime();
        long end;
        do {
            end = System.nanoTime();
        } while (start + i >= end);
    }

}
