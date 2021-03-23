package org.evrete.benchmarks.models.sales;

public class Invoice {
    public final double amount;
    public final Customer customer;
    public final Department department;

    public Invoice(double amount, Customer customer, Department department) {
        this.amount = amount;
        this.customer = customer;
        this.department = department;
    }

    public Invoice(double amount, Customer customer) {
        this.amount = amount;
        this.customer = customer;
        this.department = null;
    }
}
