package org.evrete.benchmarks.models.sales;

import java.security.SecureRandom;
import java.util.Random;

public class Customer {
    private static final Random random = new SecureRandom();

    public final int id;
    public final double rating;

    public Customer(int id) {
        this(id, random.nextInt(10) / 2.0);
    }

    private Customer(int id, double rating) {
        this.id = id;
        this.rating = rating;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                '}';
    }
}
