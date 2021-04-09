package org.evrete.benchmarks.models.sales;

public class Invoice {
    public final double amount;
    //public final Customer customer;
    public final SalesUnit salesUnit;
    public final int customerId;

    public Invoice(double amount, Customer customer, SalesUnit salesUnit) {
        this.amount = amount;
        //this.customer = customer;
        this.salesUnit = salesUnit;
        this.customerId = customer.id;
    }

    public Invoice(double amount, Customer customer) {
        this.amount = amount;
        this.salesUnit = null;
        this.customerId = customer.id;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "amount=" + amount +
                ", unit=" + salesUnit.id +
                ", customer=" + customerId +
                '}';
    }
}
