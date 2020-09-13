package org.evrete.api;

public interface OrderedServiceProvider extends Comparable<OrderedServiceProvider> {

    int order();

    @Override
    default int compareTo(OrderedServiceProvider o) {
        return Integer.compare(this.order(), o.order());
    }

}
