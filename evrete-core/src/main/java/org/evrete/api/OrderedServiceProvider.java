package org.evrete.api;

public interface OrderedServiceProvider extends Comparable<OrderedServiceProvider> {

    int sortOrder();

    @Override
    default int compareTo(OrderedServiceProvider o) {
        return Integer.compare(this.sortOrder(), o.sortOrder());
    }

}
