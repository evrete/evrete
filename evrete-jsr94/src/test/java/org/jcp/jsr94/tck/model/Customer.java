/*

 * JAVA COMMUNITY PROCESS
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck.model;

/**
 * This class defines the Customer business object that is part of the
 * JSR-94 TCK.
 * <p>
 * The Customer class is a simple class that contains the following
 * properties:<br>
 * <ul>
 * <li>name
 * <li>creditLimit
 * </ul>
 */
public class Customer {
    // The name of the customer.
    private String name;
    // The credit limit of the customer.
    private int creditLimit;

    /**
     * Create an instance of the Customer class.
     *
     * @param name The name of the customer.
     */
    public Customer(String name) {
        this.name = name;
    }

    /**
     * Get the name of this customer.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of this customer.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the credit limit of this customer.
     */
    public int getCreditLimit() {
        return this.creditLimit;
    }

    /**
     * Set the credit limit for this customer.
     */
    public void setCreditLimit(int creditLimit) {
        this.creditLimit = creditLimit;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "name='" + name + '\'' +
                ", creditLimit=" + creditLimit +
                '}';
    }
}
