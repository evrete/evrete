/*
 * J A V A  C O M M U N I T Y  P R O C E S S
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck.util;

// java imports

import org.jcp.jsr94.tck.model.Customer;
import org.jcp.jsr94.tck.model.Invoice;

import javax.rules.ObjectFilter;

/**
 * Utility class for the JSR-94 TCK.
 * <p>
 * This class implements an ObjectFilter that will be used in testing
 * the ObjectFilter functionality of rule sessions.
 * <p>
 * The filter will filter objects based on their class definition. The
 * type of the filter, specified in the constructor, will determine
 * which class will be filtered out.
 *
 * @version 1.0
 * @since JSR-94 1.0
 */
public class TestObjectFilter implements ObjectFilter {
    /**
     * Filter Customer objects.
     *
     * @see org.jcp.jsr94.tck.model.Customer
     */
    public final static int CUSTOMER_FILTER = 0;
    /**
     * Filter Invoice objects.
     *
     * @see org.jcp.jsr94.tck.model.Invoice
     */
    public final static int INVOICE_FILTER = 1;
    private static final long serialVersionUID = -3133243742600234717L;
    // The type of this filter.
    private final int filterType;

    /**
     * TestObjectFilter constructor.
     *
     * @param type The type of the object filter.
     */
    public TestObjectFilter(int type) {
        filterType = type;
    }

    /**
     * @see javax.rules.ObjectFilter#filter
     */
    public Object filter(Object object) {
        switch (filterType) {
            case CUSTOMER_FILTER: {
                if (object instanceof Customer)
                    return object;
                break;
            }
            case INVOICE_FILTER: {
                if (object instanceof Invoice)
                    return object;
                break;
            }
        }
        return null;
    }

    /**
     * @see javax.rules.ObjectFilter#reset
     */
    public void reset() {
    }
}
