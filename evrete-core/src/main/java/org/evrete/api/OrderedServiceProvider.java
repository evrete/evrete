package org.evrete.api;

/**
 * The OrderedServiceProvider interface represents a service provider interface (SPI)
 * that allows multiple implementations to be ordered based on their assigned sort order.
 */
public interface OrderedServiceProvider extends Comparable<OrderedServiceProvider> {

    /**
     * <p>
     * When several implementations are found, and no specific implementation class is given,
     * the engine will automatically pick the one with the least sorting order.
     * </p>
     *
     * @return sort order
     */
    int sortOrder();

    @Override
    default int compareTo(OrderedServiceProvider o) {
        return Integer.compare(this.sortOrder(), o.sortOrder());
    }

}
