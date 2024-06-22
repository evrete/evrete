package org.evrete.runtime;

public abstract class DeltaMemoryAction {
    private final FactHolder factHolder;
    private final ActiveType type;
    private final boolean applyToMemory;

    DeltaMemoryAction(ActiveType type, FactHolder factHolder, boolean applyToMemory) {
        this.factHolder = factHolder;
        this.type = type;
        this.applyToMemory = applyToMemory;
    }

    public boolean applyToMemory() {
        return applyToMemory;
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
        private final FactFieldValues values;

        Insert(ActiveType type, FactHolder factHolder, FactFieldValues values, boolean applyToMemory) {
            super(type, factHolder, applyToMemory);
            this.values = values;
        }

        FactFieldValues getValues() {
            return values;
        }

        @Override
        public String toString() {
            return "Insert{" +
                    "handle=" + getHandle() +
                    ", payload=" + getFactWrapper() +
                    '}';
        }

    }

    static class Delete extends DeltaMemoryAction {

        public Delete(ActiveType type, FactHolder factHolder, boolean applyToMemory) {
            super(type, factHolder, applyToMemory);
        }

        @Override
        public String toString() {
            return "Delete{" +
                    "handle=" + getHandle() +
                    ", values=" + getFactWrapper().getFieldValuesId() +
                    '}';
        }
    }
}
