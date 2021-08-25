/*
 * J A V A  C O M M U N I T Y  P R O C E S S
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck.model;

/**
 * This class defines the Invoice business object that is part of the
 * JSR-94 TCK.
 * <p>
 * The Invoice class is a simple class that contains the following
 * properties:<br>
 * <ul>
 * <li>description
 * <li>amount
 * <li>status
 * </ul>
 */
public class Invoice {
    // The description of the Invoice.
    private String description;
    // The amount to pay for the Invoice.
    private int amount;
    // The status of the Invoice.
    private String status;

    /**
     * Create an instance of the Invoice class.
     *
     * @param description The description of the invoice.
     */
    public Invoice(String description) {
        this.description = description;
        this.status = "unpaid";
    }

    /**
     * Get the description of this invoice.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set the description for this invoice.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the payment amount for this invoice.
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Set the payment amount for this invoice.
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Get the status for this invoice.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Set the status for this invoice.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "description='" + description + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
    }
}
