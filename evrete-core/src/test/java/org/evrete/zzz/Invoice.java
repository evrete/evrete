package org.evrete.zzz;

public class Invoice {
    public final double amount;
    public final Customer customer;
    public final SalesUnit salesUnit;

    public Invoice(double amount, Customer customer, SalesUnit salesUnit) {
        this.amount = amount;
        this.customer = customer;
        this.salesUnit = salesUnit;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "amount=" + amount +
                ", customer=" + customer +
                ", salesUnit=" + salesUnit +
                '}';
    }
}
