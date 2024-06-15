package org.evrete.runtime;

import java.util.Set;

public abstract class DeltaMemoryAction {
    private final DefaultFactHandle factHandle;
    private final boolean appliedToFactStorage;
    private final FactHolder factHolder;
    private final ActiveType type;

    DeltaMemoryAction(ActiveType type, DefaultFactHandle factHandle, FactHolder factHolder, boolean appliedToFactStorage) {
        this.factHandle = factHandle;
        this.appliedToFactStorage = appliedToFactStorage;
        this.factHolder = factHolder;
        this.type = type;
    }

    final boolean isAppliedToFactStorage() {
        return appliedToFactStorage;
    }

    final ActiveType getType() {
        return type;
    }

    final FactHolder getFactWrapper() {
        return factHolder;
    }

    final DefaultFactHandle getHandle() {
        return factHandle;
    }

    static class Insert extends DeltaMemoryAction {
        private final Set<AlphaAddress> destinations;
        //private final Mask<AlphaAddress> destinations;

        Insert(ActiveType type, DefaultFactHandle factHandle, boolean appliedToFactStorage, RoutedFactHolder factHolder) {
            super(type, factHandle, factHolder.getFactHolder(), appliedToFactStorage);
            this.destinations = factHolder.getDestinations();
            //this.destinations = factHolder.getDestinations();
        }


        public Set<AlphaAddress> getDestinations() {
            return destinations;
        }

        @Override
        public String toString() {
            return "Insert{" +
                    "handle=" + getHandle() +
                    ", payload=" + getFactWrapper() +
                    ", applied=" + isAppliedToFactStorage() +
                    '}';
        }

    }

//    static class Update extends DeltaMemoryAction {
//        public Update(DefaultFactHandle factHandle, boolean appliedToFactStorage) {
//            super(factHandle, appliedToFactStorage);
//        }
//    }

    static class Delete extends DeltaMemoryAction {

        public Delete(ActiveType type, DefaultFactHandle factHandle, boolean appliedToFactStorage, FactHolder factHolder) {
            super(type, factHandle, factHolder, appliedToFactStorage);
        }

        @Override
        public String toString() {
            return "Delete{" +
                    "handle=" + getHandle() +
                    ", values=" + getFactWrapper().getValues() +
                    ", applied=" + isAppliedToFactStorage() +
                    '}';
        }
    }
}
