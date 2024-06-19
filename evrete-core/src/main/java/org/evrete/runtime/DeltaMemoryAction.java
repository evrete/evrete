package org.evrete.runtime;

import java.util.Comparator;
import java.util.List;

public abstract class DeltaMemoryAction {
    private final boolean appliedToFactStorage;
    private final FactHolder factHolder;
    private final ActiveType type;

    DeltaMemoryAction(ActiveType type, FactHolder factHolder, boolean appliedToFactStorage) {
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
        return factHolder.getHandle();
    }

    static class Insert extends DeltaMemoryAction {
        private final List<AlphaAddress> destinations;
        //private final Mask<AlphaAddress> destinations;

        Insert(ActiveType type, DefaultFactHandle factHandle, boolean appliedToFactStorage, RoutedFactHolder factHolder) {
            super(type, factHolder.getFactHolder(), appliedToFactStorage);
            this.destinations = factHolder.getDestinations();
            //this.destinations = factHolder.getDestinations();
        }


        public List<AlphaAddress> getDestinations() {
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

        public Delete(ActiveType type, boolean appliedToFactStorage, FactHolder factHolder) {
            super(type, factHolder, appliedToFactStorage);
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
