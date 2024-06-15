package org.evrete.runtime;

import java.util.Set;

/**
 * A utility class that contains both the fact and the computed alpha memories it is destined for.
 * The alpha routing information is computed based on the success or failure of alpha-conditions.
 */
public class RoutedFactHolder {
    private final FactHolder factHolder;
    //private final Mask<AlphaAddress> destinations;
    private final Set<AlphaAddress> destinations;

    /**
     * Constructs an instance with the specified fact and destination.
     *
     * @param factHolder the fact in the engine's internal format.
     * @param destinations the mask of matching alpha addresses where the fact is routed to.
     */
    public RoutedFactHolder(FactHolder factHolder, Set<AlphaAddress> destinations) {
        this.factHolder = factHolder;
        //this.destinations = destinations;
        this.destinations = destinations;
    }

    public FactHolder getFactHolder() {
        return factHolder;
    }

/*
    public Mask<AlphaAddress> getDestinations() {
        return destinations;
    }
*/

    public Set<AlphaAddress> getDestinations() {
        return destinations;
    }
}
